const ENGINE_BASE = self.location.href.substring(0, self.location.href.lastIndexOf('/') + 1);

function postInfo(message) {
    self.postMessage('info string ' + message);
}

self.addEventListener('error', function(event) {
    const details = [event.message, event.filename, event.lineno, event.colno].filter(Boolean).join(' | ');
    postInfo('Worker error: ' + (details || 'unknown'));
});

self.addEventListener('unhandledrejection', function(event) {
    const reason = event.reason && event.reason.message ? event.reason.message : String(event.reason);
    postInfo('Worker rejection: ' + reason);
});

// 1. Nạp file gốc vào luồng Worker
try {
    importScripts(ENGINE_BASE + 'pikafish.js');
    postInfo('Loaded pikafish.js from ' + ENGINE_BASE);
} catch (e) {
    const msg = e && e.message ? e.message : String(e);
    postInfo('Cannot load pikafish.js: ' + msg);
    throw e;
}

// Hàng đợi lệnh để không làm mất lệnh gửi sớm khi engine chưa khởi tạo xong
let engineInstance = null;
const pendingCommands = [];

function dispatchCommand(cmd) {
    if (!cmd) return;
    if (engineInstance && typeof engineInstance.sendCommand === 'function') {
        engineInstance.sendCommand(cmd);
        return;
    }
    pendingCommands.push(cmd);
}

// Lắng nghe lệnh từ UI ngay từ đầu
self.onmessage = function(event) {
    const cmd = typeof event.data === 'string' ? event.data.trim() : '';
    dispatchCommand(cmd);
};

// 2. Khởi tạo Pikafish
// Lưu ý: Pikafish() trả về một Promise
Pikafish({
    // Bắt buộc cho build có pthread: script chính để spawn em-pthread worker
    mainScriptUrlOrBlob: ENGINE_BASE + 'pikafish.js',
    // ĐỊNH NGHĨA "TAI NGHE" TRƯỚC KHI CHẠY
    onReceiveStdout: function(text) {
        // Hễ AI nói gì là hét lên cho giao diện Web nghe ngay
        self.postMessage(text);
    },
    onReceiveStderr: function(err) {
        postInfo('AI stderr: ' + err);
    },
    // Trỏ đường dẫn file tài nguyên của engine
    locateFile: function(path, prefix) {
        if (path.endsWith('.data')) return ENGINE_BASE + 'pikafish.data';
        if (path.endsWith('.wasm')) return ENGINE_BASE + 'pikafish.wasm';
        if (path.endsWith('.js')) return ENGINE_BASE + path;
        return (prefix || ENGINE_BASE) + path;
    }
}).then(function(engine) {
    engineInstance = engine;
    postInfo('Khởi động Engine thành công!');

    while (pendingCommands.length > 0) {
        const cmd = pendingCommands.shift();
        engineInstance.sendCommand(cmd);
    }
}).catch(function(e) {
    const msg = e && e.message ? e.message : String(e);
    postInfo('Lỗi khởi tạo engine: ' + msg);
});