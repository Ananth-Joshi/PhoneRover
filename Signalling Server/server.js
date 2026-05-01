import { createServer } from "http";
import { Server } from "socket.io";

const httpServer = createServer();

const io = new Server(httpServer, {
    cors: {
        origin: "*"
    }
});

io.on("connection", (socket) => {
    console.log("connected:", socket.id);

    // join room
    socket.on("join-room", (roomId) => {
        socket.join(roomId);
        console.log(`${socket.id} joined ${roomId}`);
    });

    // relay messages inside room
    socket.on("offer", ({ roomId, offer }) => {
        socket.to(roomId).emit("offer", offer);
    });

    socket.on("answer",({ roomId , answer})=>{
        socket.to(roomId).emit("answer", answer);
    });

    socket.on("ice-candidate",({roomId,icecandidate})=>{
        socket.to(roomId).emit("ice-candidate", icecandidate);
    });

    socket.on("disconnect", () => {
        console.log("disconnected:", socket.id);
    });
});

httpServer.listen(3000, () => {
    console.log("Server Running...");
});