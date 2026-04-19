const ENGINE_BASE = self.location.href.substring(0, self.location.href.lastIndexOf('/') + 1);
importScripts(ENGINE_BASE + 'pikafish.js');

let engineInstance = null;

self.onmessage = function (event) {
    engineInstance.sendCommand(event.data);
};

Pikafish({
    mainScriptUrlOrBlob: ENGINE_BASE + 'pikafish.js',
    onReceiveStdout: function (text) {
        self.postMessage(text);
    },
}).then(function (engine) {
    engineInstance = engine;
})