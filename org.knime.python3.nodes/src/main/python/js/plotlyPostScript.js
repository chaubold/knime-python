const iFrameKnimeService = new knimeService.IFrameKnimeService();
const selectionService = new knimeService.SelectionService(iFrameKnimeService);

const d = document.getElementById('{plot_id}');
let selected = []

d.on('plotly_selected', function (data) {
    if (data === undefined) {
        selected = [];
    } else {
        selected = data.points.map(p => p.customdata[0]);
    }
    selectionService.replace(selected);
});

selectionService.addOnSelectionChangeCallback(event => {
    const keys = event.params[0].keys;
    const mode = event.params[0].mode;

    // Update the selected keys
    if (mode == "REPLACE") {
        selected = keys;
    } else if (mode == "ADD") {
        console.log("Added keys");
        selected.push(...keys);
    } else if (mode == "REMOVE") {
        // TODO Remove the keys from the "selected" Array
    }

    if (selected.length == 0) {
        // Reset selection for all traces
        Plotly.restyle(d, { selectedpoints: [null] });
    } else {
        // Loop over traces
        for (let i = 0; i < d.data.length; i++) {

            // Find the indices of the selected points for this trace
            let indices = []
            for (let j = 0; j < d.data[i].customdata.length; j++) {
                if (selected.includes(d.data[i].customdata[j][0])) {
                    indices.push(j);
                }
            }

            // Update this trace
            Plotly.restyle(d, { selectedpoints: [indices] }, [i]);
        }
    }
});