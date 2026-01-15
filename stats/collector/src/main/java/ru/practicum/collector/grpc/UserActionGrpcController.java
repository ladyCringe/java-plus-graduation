package ru.practicum.collector.grpc;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.collector.kafka.UserActionProducer;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import stats.service.collector.ActionTypeProto;
import stats.service.collector.UserActionControllerGrpc;
import stats.service.collector.UserActionProto;

import java.time.Instant;

@GrpcService
@RequiredArgsConstructor
public class UserActionGrpcController extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final UserActionProducer producer;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {

        ActionTypeAvro actionType = mapAction(request.getActionType());

        long tsMillis = 0L;
        if (request.hasTimestamp()) {
            tsMillis = Instant.ofEpochSecond(
                    request.getTimestamp().getSeconds(),
                    request.getTimestamp().getNanos()
            ).toEpochMilli();
        } else {
            tsMillis = Instant.now().toEpochMilli();
        }

        UserActionAvro avro = UserActionAvro.newBuilder()
                .setUserId(request.getUserId())
                .setEventId(request.getEventId())
                .setActionType(actionType)
                .setTimestamp(Instant.ofEpochMilli(tsMillis))
                .build();

        producer.send(avro);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    private ActionTypeAvro mapAction(ActionTypeProto proto) {
        return switch (proto) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> ActionTypeAvro.VIEW;
        };
    }
}
