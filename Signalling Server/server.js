import { createServer } from "http";
import { Server } from "socket.io";

const httpServer = createServer();
const port = 3000;
const io = new Server(httpServer, {
    cors: {
        origin: "*",
        methods: ["GET", "POST"],
        credentials: false
    }
});

io.on("connection", (socket) => {
    console.log("connected:", socket.id);

    // Join room
    socket.on("join-room", (roomId) => {
        socket.join(roomId);
        console.log(`${socket.id} joined ${roomId}`);
    });

    // 1. RELAY OFFER (Keep this exactly like this for Android's parser)
    socket.on("offer", (payload) => {
        socket.to(payload.roomId).emit("offer", payload.offer);
    });

    // 2. RELAY ANSWER (Forward the whole payload, because Android sends a flat object)
    socket.on("answer", (payload) => {
        socket.to(payload.roomId).emit("answer", payload);
    });

    // 3. RELAY ICE CANDIDATES (Forward the whole payload so no coordinates get lost)
    socket.on("ice-candidate", (payload) => {
        socket.to(payload.roomId).emit("ice-candidate", payload);
    });

    socket.on("disconnect", () => {
        console.log("disconnected:", socket.id);
    });
});

httpServer.listen(port, () => {
    console.log(`Server Running on port ${port}`);
});