/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.mcef.listeners;

import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * Stub implementation of OkHttpProgressInterceptor for native GUI migration
 * 
 * This replaces the original MCEF progress interceptor functionality with no-op stubs
 * since the native GUI doesn't require MCEF browser progress tracking.
 */
public class OkHttpProgressInterceptor implements Interceptor {
    
    public interface ProgressListener {
        void onProgress(long bytesRead, long contentLength, boolean done);
    }
    
    private final ProgressListener listener;
    
    public OkHttpProgressInterceptor(ProgressListener listener) {
        this.listener = listener;
    }
    
    @Override
    public Response intercept(Chain chain) throws java.io.IOException {
        okhttp3.Request request = chain.request();
        Response response = chain.proceed(request);
        
        // In the original implementation, this would track download progress
        // For the stub, we just pass through the response without progress tracking
        
        return response;
    }
}