Service Governance
===

## 1. Service Registration

### 1.1 Service Registration

During the application startup process, the registration plugin intercepts and retrieves the initialization methods of consumers and service providers, modifies their metadata, and adds tags for multi-active and lane governance.

Subsequently, when registering with the registry, these tags will be included.

### 1.2 Service Subscription

1. During the service registration process, the service will subscribe to its related governance strategies.
2. In the proxy microservice configuration, you can configure the names of services to be warmed up. During the proxy startup process, it will pre-subscribe to the related governance strategies.

### 1.3 Graceful Startup

1. Intercept registry events and put the registration into a delay queue.
2. Intercept the application lifecycle, synchronously waiting for the subscribed service strategies to be ready before the application service is ready.
3. In the traffic control-related plugins, incoming requests will be judged, and if the proxy is not yet ready, the request will be rejected.
4. After the synchronization of service strategies is completed, register again when the proxy is ready.
5. Ensure that the consumer gets the address only after the backend service is ready.

### 1.4 Graceful Shutdown

1. Use system hooks to intercept the shutdown event and set the proxy status to closed.
2. In the traffic control-related plugins, incoming requests will be judged, and if the proxy is not yet ready, the request will be rejected.
3. Ensure that new requests are not processed during the shutdown process.

## 2. Multi-Active Traffic Governance

## 3. Lane Governance

## 4. Microservice Governance

### 4.1 Cluster Strategy

### 4.2 Rate Limiting

### 4.3 Load Balancing

### 4.4 Tag Routing

## 5. Graceful Startup and Shutdown