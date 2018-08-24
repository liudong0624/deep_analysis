package wa;

import grpc.health.v1.GRPCh;
import grpc.health.v1.HealthGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealthCheck extends HealthGrpc.HealthImplBase {
    private static Logger logger = LoggerFactory.getLogger(HealthCheck.class);
    public void check(grpc.health.v1.GRPCh.HealthCheckRequest request,
                      io.grpc.stub.StreamObserver<grpc.health.v1.GRPCh.HealthCheckResponse> responseObserver) {
        String service = request.getService();
        logger.debug("服务{}查询健康状态",service);
        GRPCh.HealthCheckResponse response =
                GRPCh.HealthCheckResponse.newBuilder().
                        setStatusValue(1).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
