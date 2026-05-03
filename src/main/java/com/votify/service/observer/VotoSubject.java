package com.votify.service.observer;

import com.votify.entity.Voting;

public interface VotoSubject {
    // 
    void addObserver(VotoObserver observer);
    //
    void removeObserver(VotoObserver observer);
    void notifyObservers(Voting voting);
}
