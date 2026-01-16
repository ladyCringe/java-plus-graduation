package ru.practicum.collector.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.collector.service.UserActionHandler;
import stats.service.collector.UserActionControllerGrpc;
import stats.service.collector.UserActionProto;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserActionController extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final UserActionHandler userActionHandler;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        try {
            userActionHandler.handle(request);
            respondOk(responseObserver);
        } catch (Exception e) {
            respondInternalError(responseObserver, e);
        }
    }

    private void respondOk(StreamObserver<Empty> responseObserver) {
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    private void respondInternalError(StreamObserver<Empty> responseObserver, Exception e) {
        responseObserver.onError(new StatusRuntimeException(
                Status.INTERNAL
                        .withDescription(e.getLocalizedMessage())
                        .withCause(e)
        ));
    }
}
