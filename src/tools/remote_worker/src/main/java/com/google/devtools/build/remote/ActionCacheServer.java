// Copyright 2017 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.remote;

import static java.util.logging.Level.WARNING;

import com.google.devtools.build.lib.remote.Digests;
import com.google.devtools.build.lib.remote.Digests.ActionKey;
import com.google.devtools.build.lib.remote.SimpleBlobStoreActionCache;
import com.google.devtools.remoteexecution.v1test.ActionCacheGrpc.ActionCacheImplBase;
import com.google.devtools.remoteexecution.v1test.ActionResult;
import com.google.devtools.remoteexecution.v1test.GetActionResultRequest;
import com.google.devtools.remoteexecution.v1test.UpdateActionResultRequest;
import io.grpc.stub.StreamObserver;
import java.util.logging.Logger;

/** A basic implementation of an {@link ActionCacheImplBase} service. */
final class ActionCacheServer extends ActionCacheImplBase {
  private static final Logger logger = Logger.getLogger(ActionCacheImplBase.class.getName());

  private final SimpleBlobStoreActionCache cache;

  public ActionCacheServer(SimpleBlobStoreActionCache cache) {
    this.cache = cache;
  }

  @Override
  public void getActionResult(
      GetActionResultRequest request, StreamObserver<ActionResult> responseObserver) {
    try {
      ActionKey actionKey = Digests.unsafeActionKeyFromDigest(request.getActionDigest());
      ActionResult result = cache.getCachedActionResult(actionKey);

      if (result == null) {
        responseObserver.onError(StatusUtils.notFoundError(request.getActionDigest()));
        return;
      }

      responseObserver.onNext(result);
      responseObserver.onCompleted();
    } catch (Exception e) {
      logger.log(WARNING, "getActionResult request failed.", e);
      responseObserver.onError(StatusUtils.internalError(e));
    }
  }

  @Override
  public void updateActionResult(
      UpdateActionResultRequest request, StreamObserver<ActionResult> responseObserver) {
    try {
      ActionKey actionKey = Digests.unsafeActionKeyFromDigest(request.getActionDigest());
      cache.setCachedActionResult(actionKey, request.getActionResult());
      responseObserver.onNext(request.getActionResult());
      responseObserver.onCompleted();
    } catch (Exception e) {
      logger.log(WARNING, "updateActionResult request failed.", e);
      responseObserver.onError(StatusUtils.internalError(e));
    }
  }
}
