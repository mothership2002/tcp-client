# Netty 4.2 Migration Guide

## Overview

This project has been updated to use Netty 4.2.x API, which is the version bundled with Spring Boot 4.0.1.

## Changes Made

### 1. Deprecated API Replacement

**Before (Netty 4.1.x style):**
```java
import io.netty.channel.nio.NioEventLoopGroup;

EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
```

**After (Netty 4.2.x style):**
```java
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;

EventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
```

### 2. Files Modified

1. **TcpClientAutoConfiguration.java**
   - Updated imports to use `MultiThreadIoEventLoopGroup` and `NioIoHandler`
   - Changed `eventLoopGroup()` bean creation method
   - Added documentation about Netty 4.2+ usage

2. **TcpClientE2ETest.java**
   - Updated test setup to use new API
   - Ensures E2E tests work with Netty 4.2.x

### 3. Why This Change?

- **Spring Boot 4.0.1 Dependency**: Spring Boot 4.0.1 uses Netty 4.2.9.Final
- **Deprecation Warning**: `NioEventLoopGroup` is deprecated in Netty 4.2.x
- **Future-Proofing**: Using the recommended API ensures compatibility with future Netty versions
- **Clean Compilation**: Eliminates deprecation warnings during build

### 4. Behavioral Impact

**No functional changes:**
- Default thread count remains the same (CPU cores * 2)
- Same EventLoopGroup interface is used
- All existing functionality continues to work
- All tests pass without modifications

### 5. Verification

```bash
# No deprecation warnings
./gradlew clean compileJava compileTestJava

# All tests pass
./gradlew test

# Full build succeeds
./gradlew clean build
```

## References

- [Netty 4.2 Migration Guide](https://netty.io/wiki/netty-4.2-migration-guide.html)
- [NioEventLoopGroup Deprecation Notice](https://netty.io/4.2/api/io/netty/channel/nio/NioEventLoopGroup.html)
- [MultiThreadIoEventLoopGroup API](https://netty.io/4.2/api/io/netty/channel/MultiThreadIoEventLoopGroup.html)
- [NioIoHandler API](https://netty.io/4.2/api/io/netty/channel/nio/NioIoHandler.html)

## Recommendation for Users

If you're providing your own `EventLoopGroup` bean, update it to use the new API:

```java
@Bean
public EventLoopGroup customEventLoopGroup() {
    // Old way (deprecated)
    // return new NioEventLoopGroup(4);
    
    // New way (recommended)
    return new MultiThreadIoEventLoopGroup(4, NioIoHandler.newFactory());
}
```
