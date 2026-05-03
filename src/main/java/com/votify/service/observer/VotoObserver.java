package com.votify.service.observer;

import com.votify.entity.Voting;

public interface VotoObserver {
    void onVotoGuardado(Voting voting);
}
