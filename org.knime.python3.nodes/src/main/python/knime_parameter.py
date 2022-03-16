

class Parameter:

    def __init__(self, default_fn, doc=None, validator=None):
        self._validator = validator
        self._default_fn = default_fn

        if doc is None and default_fn is not None:
            doc = default_fn.__doc__
        self.__doc__ = doc

    def __set_name__(self, owner, name):
        self._name = name

    def __get__(self, obj, type=None):
        if obj is None:
            return self
        self.create_params_dict_if_missing(obj)
        if self._name not in obj.__knime_parameters__:
            obj.__knime_parameters__[self._name] = self._default_fn(obj)
        return obj.__knime_parameters__[self._name]

    def create_params_dict_if_missing(self, obj):
        if not hasattr(obj, "__knime_parameters__"):
            obj.__knime_parameters__ = {} 

    def __set__(self, obj, value):
        self.validate(obj, value)
        print(f"Setting parameter {self._name} of object {obj} to {value}")
        self.create_params_dict_if_missing(obj)
        obj.__knime_parameters__[self._name] = value

    def validate(self, obj, value):
        if self._validator is not None:
            self._validator(obj, value)

    def validator(self, validator):
        return Parameter(default_fn=self._default_fn, doc=self.__doc__, validator=validator)
