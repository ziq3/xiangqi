<%@ page contentType="text/html;charset=UTF-8" %>
    <!DOCTYPE html>
    <html>

    <head>
        <title>Cờ tướng</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/xiangqiboard-0.3.3.min.css">
    </head>
    <style>
        #myBoard {
            width: min(88vh, 100vw)
        }

        .top-right {
            position: fixed;
            top: 12px;
            right: 12px;
            z-index: 1000;
        }
    </style>

    <body>
        <div id="myBoard"></div>
        <script src="https://code.jquery.com/jquery-3.5.1.min.js"></script>
        <script src="${pageContext.request.contextPath}/js/xiangqiboard-0.3.3.min.js"></script>
        <a class="login-btn top-right" href="${pageContext.request.contextPath}/login">Login</a>
        <script>


            const BASE = '${pageContext.request.contextPath}';
            let currentMoveHistory = "";
            let engineWorker = new Worker(BASE + '/js/wrapper.js');

            function sendCommand(cmd) {
                console.log(cmd);
                engineWorker.postMessage(cmd);
            }


            engineWorker.onmessage = function (event) {
                const line = event.data;
                console.log(line);
                if (line.startsWith("bestmove")) {
                    let bestMove = line.split(" ")[1];
                    currentMoveHistory += " " + bestMove;
                    let moveForBoard = bestMove.substring(0, 2) + '-' + bestMove.substring(2, 4);
                    board.move(moveForBoard);
                }
            };

            function onDrop(source, target) {
                let userMove = source + target;
                currentMoveHistory += " " + userMove;
                sendCommand("position startpos moves " + currentMoveHistory);
                sendCommand("go movetime 500");
            }

            const config = {
                position: 'start',
                draggable: true,
                pieceTheme: BASE + '/img/xiangqipieces/wikimedia/{piece}.svg',
                onDrop: onDrop
            }

            const board = Xiangqiboard('myBoard', config)

        </script>

    </body>

    </html>