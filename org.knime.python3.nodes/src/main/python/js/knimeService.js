var knimeService = (function (exports) {
    'use strict';

    const jsonRpcResponseHandler = (response) => {
        const { error, result } = response || {};
        if (error) {
            return Promise.reject(new Error(`Error code: ${error.code || 'UNKNOWN'}. Message: ${error.message || 'not provided'}`));
        }
        return Promise.resolve(result);
    };

    /**
     * The main API entry point base class for UI Extensions, derived class being initialized depending on environment
     * and handles all of the communication between the environment (e.g. KNIME Analytics Platform) and the registered services.
     *
     * To utilize this functionality, services should be registered with an instance of derived class, after which their
     * functionality can be utilized by the UI Extension implementation.
     *
     * Derived classes: IFrameKnimeService - for usage with iframe extensions, ComponentKnimeService for usage with components.
     *
     * @template T - the {@type ExtensionConfig} generic type.
     */
    class KnimeService {
        /**
         * @param {ExtensionConfig} extensionConfig - the extension configuration for the associated UI Extension.
         * @param {CallableService} callableService - the environment-specific "call service" API method.
         * @param {CallableService} pushNotification - the environment-specific "push notification" API method.
         */
        constructor(extensionConfig = null, callableService = null, pushNotification = null) {
            /**
             *
             */
            this.extensionConfig = extensionConfig;
            this.callableService = callableService;
            this.callablePushNotification = pushNotification;
            /**
             * Stores registered callbacks for notifications called via backend implementation.
             * Should be only used by internal service methods.
             */
            this.notificationCallbacksMap = new Map();
        }
        /**
         * Public service call wrapper with error handling which can be used by subclasses/typed service implementations.
         *
         * @param {JsonRpcRequest} jsonRpcRequest - the formatted request payload.
         * @returns {Promise} - rejected or resolved depending on response success.
         */
        callService(jsonRpcRequest) {
            if (!this.extensionConfig) {
                return Promise.reject(new Error('Cannot call service without extension config'));
            }
            if (!this.callableService) {
                return Promise.reject(new Error('Callable service is not available'));
            }
            return this.executeServiceCall(jsonRpcRequest).then(jsonRpcResponseHandler);
        }
        /**
         * Inner service call wrapper which can be overridden by subclasses which require specific behavior (e.g. iframes).
         * Default behavior is to use the member callable service directly.
         *
         * @param {JsonRpcRequest} jsonRpcRequest - the formatted request payload.
         * @returns {Promise} - rejected or resolved depending on response success.
         */
        executeServiceCall(jsonRpcRequest) {
            return this.callableService(jsonRpcRequest);
        }
        /**
         * Register a callback method which returns relevant data to provide when "applying" client-side state
         * changes to the framework (i.e. when settings change and should be persisted).
         *
         * @param {Function} callback - method which returns any data needed by the framework to persist the client-
         *      side state.
         * @returns {undefined}
         */
        registerDataGetter(callback) {
            this.dataGetter = callback;
        }
        /**
         * A framework method to get any data which is needed for state persistence. Not intended to be called directly
         * by a UI Extension implementation, this method is exposed for lifecycle management by the framework.
         *
         * @returns {any | null} optionally returns data needed to persist client side state if a
         *      {@see KnimeService.dataGetter} has been registered. If no data getter is present,
         *      returns {@type null}.
         */
        getData() {
            return Promise.resolve(typeof this.dataGetter === 'function' ? this.dataGetter() : null);
        }
        /**
         * To be called by the parent application to sent a notification to all services. Calls registered callbacks by
         * notification type.
         * @param {Notification} notification - notification object, which is provided by backend implementation.
         * @returns {void}
         */
        onJsonRpcNotification(notification) {
            const callbacks = this.notificationCallbacksMap.get(notification.method) || [];
            callbacks.forEach(callback => {
                callback(notification);
            });
        }
        /**
         * Registers callback that will be triggered on received notification.
         * @param {EventTypes} notificationType - notification type that callback should be registered for.
         * @param {function} callback - callback that should be called on received notification, will be called with {Notification} param
         * @returns {void}
         */
        addNotificationCallback(notificationType, callback) {
            this.notificationCallbacksMap.set(notificationType, [
                ...this.notificationCallbacksMap.get(notificationType) || [],
                callback
            ]);
        }
        /**
         * Unregisters previously registered callback for notifications.
         * @param {EventTypes} notificationType - notification type that matches registered callback notification type.
         * @param {function} callback - previously registered callback.
         * @returns {void}
         */
        removeNotificationCallback(notificationType, callback) {
            this.notificationCallbacksMap.set(notificationType, (this.notificationCallbacksMap.get(notificationType) || []).filter((cb) => cb !== callback));
        }
        /**
         * Unregisters all previously registered notification callbacks of provided notification type.
         * @param {string} notificationType - notification type that matches registered callbacks notification type.
         * @returns {void}
         */
        resetNotificationCallbacksByType(notificationType) {
            this.notificationCallbacksMap.set(notificationType, []);
        }
        /**
         * Unregisters all previously registered notification callbacks of all notifications types.
         * @returns {void}
         */
        resetNotificationCallbacks() {
            this.notificationCallbacksMap.clear();
        }
        /**
         * Public push notification wrapper with error handling. This broadcasts an event or notifications
         * via the callable function provided during instantiation.
         *
         * @param {Notification} notification - the notification payload.
         * @returns {any} - the result of the callable function.
         */
        pushNotification(notification) {
            if (!this.extensionConfig) {
                return Promise.reject(new Error('Cannot push notification without extension config'));
            }
            if (!this.callablePushNotification) {
                return Promise.reject(new Error('Push notification is not available'));
            }
            return this.callablePushNotification(Object.assign({ callerId: this.serviceId }, notification));
        }
        /**
         * Creates an instance ID from a @type {KnimeService}. This ID unique among node instances in a workflow but shared
         * between KnimeService instances instantiated by the same node instance (i.e. between sessions, refreshes, reloads,
         * etc.).
         *
         * @param {KnimeService} knimeService - the service from which to derive an ID.
         * @returns {String} the id derived from the provided service.
         */
        get serviceId() {
            const { nodeId, projectId, workflowId, extensionType } = this.extensionConfig || {};
            return `${nodeId}.${projectId}.${workflowId}.${extensionType}`;
        }
    }

    /**
     * Collection of node service method signatures registered as RPC services with the framework. Each signature
     * targets specific workflow-level RPC node service functionality for UI Extensions.
     */
    var NodeServiceMethods;
    (function (NodeServiceMethods) {
        // Data service method signature.
        NodeServiceMethods["CALL_NODE_DATA_SERVICE"] = "NodeService.callNodeDataService";
        // Selection service method signature.
        NodeServiceMethods["CALL_NODE_SELECTION_SERVICE"] = "NodeService.updateDataPointSelection";
    })(NodeServiceMethods || (NodeServiceMethods = {}));

    /**
     * Selection service types for the `NodeService.selectDataPoints` method.
     *
     * @enum {string}
     */
    var SelectionServiceTypes;
    (function (SelectionServiceTypes) {
        SelectionServiceTypes["ADD"] = "ADD";
        SelectionServiceTypes["REMOVE"] = "REMOVE";
        SelectionServiceTypes["REPLACE"] = "REPLACE";
    })(SelectionServiceTypes || (SelectionServiceTypes = {}));
    /**
     * Service types for DataServices implemented by a specific UI Extension node.
     *
     * @enum {string}
     *
     * TODO: NXT-761 convert to interfaces which are then members of the <Type>DataService implementations.
     */
    var DataServiceTypes;
    (function (DataServiceTypes) {
        // Returns the initial data as provided by the node implementation. Requires no parameters.
        DataServiceTypes["INITIAL_DATA"] = "initial_data";
        // Expects request to be valid RPC format to retrieve data from the referenced data service method.
        DataServiceTypes["DATA"] = "data";
        // Expects request body to contain the update data to apply/persist/update depending on node implementation.
        DataServiceTypes["APPLY_DATA"] = "apply_data";
    })(DataServiceTypes || (DataServiceTypes = {}));

    /**
     * Denotes whether the parent configuration references a node dialog or node view. As both are ui-extensions,
     * how they are rendered in a layout is determined by this type.
     *
     * @enum
     */
    var ExtensionTypes;
    (function (ExtensionTypes) {
        ExtensionTypes["DIALOG"] = "dialog";
        ExtensionTypes["VIEW"] = "view";
    })(ExtensionTypes || (ExtensionTypes = {}));

    var EventTypes;
    (function (EventTypes) {
        EventTypes["DataEvent"] = "DataEvent";
        EventTypes["SelectionEvent"] = "SelectionEvent";
    })(EventTypes || (EventTypes = {}));

    /**
     * Enum for extension resource types.
     * @readonly
     * @enum {string}
     */
    var ResourceTypes;
    (function (ResourceTypes) {
        /** Indicates the resource should be loaded as a complete HTML page. */
        ResourceTypes["HTML"] = "HTML";
        /** Indicates the resource is a Vue component and should be treated as a library. */
        ResourceTypes["VUE_COMPONENT_LIB"] = "VUE_COMPONENT_LIB";
    })(ResourceTypes || (ResourceTypes = {}));

    let requestId = 0;
    // for now we only need any kind of id, not even unique, later will need unique ones
    const generateRequestId = () => {
        requestId += 1;
        return requestId;
    };

    /**
     * KNIME Analytics Platform constant.
     */
    const JSON_RPC_VERSION = '2.0';
    const UI_EXT_POST_MESSAGE_PREFIX = 'knimeUIExtension';
    const UI_EXT_POST_MESSAGE_TIMEOUT = 10000; // 10s

    const createJsonRpcRequest = (method, params = []) => ({
        jsonrpc: JSON_RPC_VERSION,
        method,
        params,
        id: generateRequestId()
    });

    /**
     * A utility class to interact with JsonDataServices implemented by a UI Extension node.
     */
    class JsonDataService {
        /**
         * @param {KnimeService<T> | IFrameKnimeService} knimeService - knimeService instance which is used to communicate with the framework.
         */
        constructor(knimeService) {
            this.knimeService = knimeService;
        }
        /**
         * Calls a node's {@see DataService} with optional request body. The service to call is specified by the
         * service type and needs to correspond directly to a {@see DataService} implemented by the node. For
         * known service types, {@see DataServiceTypes}.
         *
         * @param {DataServiceTypes} dataService - the target service.
         * @param {string} [request] - an optional request payload.
         * @returns {Promise} rejected or resolved depending on backend response.
         */
        callDataService(dataService, request = '') {
            return this.knimeService
                .callService(createJsonRpcRequest(NodeServiceMethods.CALL_NODE_DATA_SERVICE, [
                this.knimeService.extensionConfig.projectId,
                this.knimeService.extensionConfig.workflowId,
                this.knimeService.extensionConfig.nodeId,
                this.knimeService.extensionConfig.extensionType,
                dataService,
                request || ''
            ])).then((response) => response && typeof response === 'string' ? JSON.parse(response) : response);
        }
        /**
         * Retrieves the initial data for the client-side UI Extension implementation from either the local configuration
         * (if it exists) or by fetching the data from the node DataService implementation.
         *
         * @returns {Promise} node initial data provided by the local configuration or by fetching from the DataService.
         */
        initialData() {
            var _a;
            const initialData = ((_a = this.knimeService.extensionConfig) === null || _a === void 0 ? void 0 : _a.initialData) || null;
            if (initialData) {
                return Promise.resolve(initialData)
                    .then((response) => typeof response === 'string' ? JSON.parse(response) : response);
            }
            return this.callDataService(DataServiceTypes.INITIAL_DATA);
        }
        /**
         * Retrieve data from the node using the {@see DataServiceTypes.DATA} api. Different method names can be registered
         * with the data service in the node implementation to provide targets (specified by the {@param method}). Any
         * optional parameter will be provided directly to the data service target and can be used to specify the nature of
         * the data returned.
         *
         * @param {Object} params - parameter options.
         * @param {string} [params.method] - optional target method in the node's DataService implementation
         *      (default 'getData').
         * @param {any} [params.options] - optional options that should be passed to called method.
         * @returns {Promise} rejected or resolved depending on backend response.
         */
        data(params = {}) {
            return this.callDataService(DataServiceTypes.DATA, JSON.stringify(createJsonRpcRequest(params.method || 'getData', params.options)));
        }
        /**
         * Sends the current client-side data to the backend to be persisted. A data getter method which returns the
         * data to be applied/saved should be registered *prior* to invoking this method. If none is registered, a
         * default payload of "null" will be sent instead.
         *
         * @returns {Promise} rejected or resolved depending on backend response.
         */
        async applyData() {
            const data = await this.knimeService.getData();
            return this.callDataService(DataServiceTypes.APPLY_DATA, data);
        }
        /**
         * Registers a function with the framework is used to provide the current state of the client-side UI Extension.
         *
         * @param {Function} callback - function which provides the current client side state when invoked.
         * @returns {undefined}
         */
        registerDataGetter(callback) {
            this.knimeService.registerDataGetter(() => JSON.stringify(callback()));
        }
        /**
         * Adds callback that will be triggered when data changes.
         * @param {Function} callback - called on data change.
         * @param {Notification} response - the data update event object.
         * @returns {void}
         */
        addOnDataChangeCallback(callback) {
            this.knimeService.addNotificationCallback(EventTypes.DataEvent, callback);
        }
        /**
         * Publish a data update notification to other UIExtensions registered in the current page.
         * @param {any} data - the data to send.
         * @returns {void}
         */
        publishData(data) {
            this.knimeService.pushNotification({
                method: EventTypes.DataEvent,
                event: { data }
            });
        }
    }

    /**
     * The main API entry point for IFrame-based UI extensions. Handles all communication between the extension
     * IFrame and parent window via window.postMessage.
     *
     * The parent window needs to have a instance of IFrameKnimeServiceAdapter.
     *
     * Other services should be initialized with instance of the class.
     */
    class IFrameKnimeService extends KnimeService {
        constructor() {
            super();
            this.pendingJsonRpcRequests = new Map();
            // to allow awaiting the initialization via waitForInitialization()
            // TODO NXTEXT-135 remove the need for this
            this.initializationPromise = new Promise((resolve) => {
                this.initializationPromiseResolve = resolve;
            });
            if (this.extensionConfig) {
                this.initializationPromiseResolve();
            }
            this.callableService = this.executeServiceCall;
            this.boundOnMessageFromParent = this.onMessageFromParent.bind(this);
            window.addEventListener('message', this.boundOnMessageFromParent);
            window.parent.postMessage({
                type: `${UI_EXT_POST_MESSAGE_PREFIX}:ready`
            }, '*'); // TODO NXT-793 security
        }
        /**
         * Needs to be awaited before the service is ready to be used.
         * @returns {void}
         */
        async waitForInitialization() {
            await this.initializationPromise;
        }
        /**
         * Called when a new message is received, identifies and handles it if type is supported.
         * @param {MessageEvent} event - postMessage event that is sent by parent window with event type and payload.
         * @returns {void}
         */
        onMessageFromParent(event) {
            var _a;
            // TODO NXT-793 security
            const { data } = event;
            if (!((_a = data.type) === null || _a === void 0 ? void 0 : _a.startsWith(UI_EXT_POST_MESSAGE_PREFIX))) {
                return;
            }
            switch (data.type) {
                case `${UI_EXT_POST_MESSAGE_PREFIX}:init`:
                    this.onInit(data);
                    break;
                case `${UI_EXT_POST_MESSAGE_PREFIX}:jsonrpcResponse`:
                    this.onJsonRpcResponse(data);
                    break;
                case `${UI_EXT_POST_MESSAGE_PREFIX}:jsonrpcNotification`:
                    this.onJsonRpcNotification(data.payload);
                    break;
            }
        }
        onInit(data) {
            this.extensionConfig = data.payload;
            this.initializationPromiseResolve();
        }
        onJsonRpcResponse(data) {
            const { payload: { response, requestId } } = data;
            const request = this.pendingJsonRpcRequests.get(requestId);
            if (!request) {
                throw new Error(`Received jsonrpcResponse for non-existing pending request with id ${requestId}`);
            }
            request.resolve(JSON.parse(response));
            this.pendingJsonRpcRequests.delete(requestId);
        }
        /**
         * Overrides method of KnimeService to implement how request should be processed in IFrame environment.
         * @param {JsonRpcRequest} jsonRpcRequest - to be executed by KnimeService callService method.
         * @returns {Promise<string>} - promise that resolves with JsonRpcResponse string or error message.
         */
        executeServiceCall(jsonRpcRequest) {
            let rejectTimeoutId;
            const promise = new Promise((resolve, reject) => {
                const { id } = jsonRpcRequest;
                this.pendingJsonRpcRequests.set(id, { resolve, reject });
                rejectTimeoutId = setTimeout(() => {
                    resolve(JSON.stringify({
                        error: {
                            message: `Request with id ${id} aborted due to timeout.`,
                            code: 'req-timeout'
                        },
                        result: null
                    }));
                }, UI_EXT_POST_MESSAGE_TIMEOUT);
            });
            // clearing reject timeout on promise resolve
            promise.then(() => {
                clearTimeout(rejectTimeoutId);
            });
            window.parent.postMessage({
                type: `${UI_EXT_POST_MESSAGE_PREFIX}:jsonrpcRequest`,
                payload: jsonRpcRequest
            }, '*'); // TODO NXT-793 security
            return promise;
        }
        /**
         * Should be called before destroying IFrameKnimeService, to remove event listeners from window object,
         * preventing memory leaks and unexpected behavior.
         * @returns {void}
         */
        destroy() {
            window.removeEventListener('message', this.boundOnMessageFromParent);
        }
    }

    /**
     * Handles postMessage communication with iframes on side of the parent window.
     *
     * IFrame window communication should be setup with instance of IFrameKnimeService.
     *
     * Should be instantiated by class that persists at root window object.
     */
    class IFrameKnimeServiceAdapter extends KnimeService {
        constructor(extensionConfig = null, callableService = null) {
            super(extensionConfig, callableService);
            this.boundOnMessageFromIFrame = this.onMessageFromIFrame.bind(this);
            window.addEventListener('message', this.boundOnMessageFromIFrame);
        }
        onJsonRpcNotification(notification) {
            this.iFrameWindow.postMessage({
                type: `${UI_EXT_POST_MESSAGE_PREFIX}:jsonrpcNotification`,
                payload: typeof notification === 'string' ? JSON.parse(notification) : notification
            }, '*');
        }
        /**
         * Sets the child iframe window referenced by the service.
         *
         * @param {Window} iFrameWindow - the content window of the child frame where the @see IFrameKnimeService
         *      is running.
         * @returns {void}
         */
        setIFrameWindow(iFrameWindow) {
            this.iFrameWindow = iFrameWindow;
        }
        /**
         * Checks if message is coming from the correct IFrame and therefore is secure.
         * @param {MessageEvent} event - postMessage event.
         * @returns {boolean} - returns true if postMessage source is secure.
         */
        checkMessageSource(event) {
            return event.source !== this.iFrameWindow;
        }
        /**
         * Listens for postMessage events, identifies and handles them if event type is supported.
         * @param {MessageEvent} event - postMessage event that is sent by parent window with event type and payload.
         * @returns {void}
         */
        async onMessageFromIFrame(event) {
            if (this.checkMessageSource(event)) {
                return;
            }
            const { data } = event;
            switch (data.type) {
                case `${UI_EXT_POST_MESSAGE_PREFIX}:ready`:
                    this.iFrameWindow.postMessage({
                        type: `${UI_EXT_POST_MESSAGE_PREFIX}:init`,
                        payload: this.extensionConfig
                    }, '*');
                    break;
                case `${UI_EXT_POST_MESSAGE_PREFIX}:jsonrpcRequest`:
                    {
                        const { payload } = data;
                        const requestId = payload === null || payload === void 0 ? void 0 : payload.id;
                        const response = await this.callService(payload);
                        this.iFrameWindow.postMessage({
                            type: `${UI_EXT_POST_MESSAGE_PREFIX}:jsonrpcResponse`,
                            payload: {
                                response,
                                requestId
                            }
                        }, '*');
                    }
                    break;
            }
        }
        /**
         * Should be called before destroying the IFrame to remove event listeners from window object,
         * preventing memory leaks and unexpected behavior.
         * @returns {void}
         */
        destroy() {
            window.removeEventListener('message', this.boundOnMessageFromIFrame);
            this.iFrameWindow = null;
        }
    }

    /**
     * SelectionService provides methods to handle data selection.
     * To use it, the relating Java implementation also needs to use the SelectionService.
     */
    class SelectionService {
        /**
         * @param {KnimeService} knimeService - instance should be provided to use notifications.
         */
        constructor(knimeService) {
            this.knimeService = knimeService;
        }
        /**
         * Calls the NodeService `selectDataPoints` method with request body. The selection service to call is
         * specified by the service type and needs to correspond directly to a {@see SelectionServiceTypes}.
         *
         * @param {SelectionServiceTypes} selectionService - the target selection service.
         * @param {string} request - the request payload.
         * @returns {Promise} rejected or resolved depending on backend response.
         */
        callSelectionService(selectionService, request) {
            return this.knimeService
                .callService(createJsonRpcRequest(NodeServiceMethods.CALL_NODE_SELECTION_SERVICE, [
                this.knimeService.extensionConfig.projectId,
                this.knimeService.extensionConfig.workflowId,
                this.knimeService.extensionConfig.nodeId,
                selectionService,
                request || ''
            ])).then((response) => typeof response === 'string' ? JSON.parse(response) : response);
        }
        /**
         * Adds data to currently selected data set.
         * @param {(string | key)[]} keys - will be passed as params to backend SelectionService add selection method
         * @returns {Promise<Object>} based on backend implementation.
         */
        add(keys) {
            return this.callSelectionService(SelectionServiceTypes.ADD, keys);
        }
        /**
         * Removes data from currently selected data set.
         * @param {(string | key)[]} keys - will be passed as params to backend SelectionService remove selection method.
         * @returns {Promise<Object>} - based on backend implementation.
         */
        remove(keys) {
            return this.callSelectionService(SelectionServiceTypes.REMOVE, keys);
        }
        /**
         * Replaces current selection with provided data.
         * @param {(string | key)[]} keys - will be passed as params to backend SelectionService replace selection method.
         * @returns {Promise<Object>} - based on backend implementation.
         */
        replace(keys) {
            return this.callSelectionService(SelectionServiceTypes.REPLACE, keys);
        }
        /**
         * Adds callback that will be triggered on data selection change by backend.
         * @param {function} callback - that need to be added. Will be triggered by backend implementation on selection change.
         * @param {Notification} response - object that backend will trigger callback with.
         * @returns {void}
         */
        addOnSelectionChangeCallback(callback) {
            this.knimeService.addNotificationCallback(EventTypes.SelectionEvent, callback);
        }
        /**
         * Removes previously added callback.
         * @param {function} callback - that needs to be removed from notifications.
         * @param {Notification} response - object that backend will trigger callback with.
         * @returns {void}
         */
        removeOnSelectionChangeCallback(callback) {
            this.knimeService.removeNotificationCallback(EventTypes.SelectionEvent, callback);
        }
    }

    exports.IFrameKnimeService = IFrameKnimeService;
    exports.IFrameKnimeServiceAdapter = IFrameKnimeServiceAdapter;
    exports.JsonDataService = JsonDataService;
    exports.KnimeService = KnimeService;
    exports.SelectionService = SelectionService;
    exports.UI_EXT_POST_MESSAGE_PREFIX = UI_EXT_POST_MESSAGE_PREFIX;

    Object.defineProperty(exports, '__esModule', { value: true });

    return exports;

})({});
