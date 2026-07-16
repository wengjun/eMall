package com.emall.loadtest;

import java.util.concurrent.CompletionStage;

interface RequestDispatcher {
    CompletionStage<RequestResult> dispatch(long globalSequence, LoadPattern.StageDefinition stage);
}
