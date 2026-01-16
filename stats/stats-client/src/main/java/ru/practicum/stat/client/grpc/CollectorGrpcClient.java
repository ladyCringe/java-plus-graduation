package ru.practicum.stat.client.grpc;

import com.google.protobuf.Timestamp;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import stats.service.collector.*;

import java.time.Instant;

@Component
public class CollectorGrpcClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub client;

    public void sendView(long userId, long eventId) {
        send(userId, eventId, ActionTypeProto.ACTION_VIEW);
    }

    public void sendRegister(long userId, long eventId) {
        send(userId, eventId, ActionTypeProto.ACTION_REGISTER);
    }

    public void sendLike(long userId, long eventId) {
        send(userId, eventId, ActionTypeProto.ACTION_LIKE);
    }

    private void send(long userId, long eventId, ActionTypeProto type) {
        Instant now = Instant.now();
        Timestamp ts = Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();

        UserActionProto req = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(type)
                .setTimestamp(ts)
                .build();

        client.collectUserAction(req);
    }
}

