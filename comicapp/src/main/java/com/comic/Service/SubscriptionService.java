package com.comic.Service;

import com.comic.Repository.SubscriptionRepository;
import com.comic.model.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("SubscriptionService")
public class SubscriptionService {

    private SubscriptionRepository subscriptionRepository;

    @Autowired
    public SubscriptionService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public Subscription findIfSubscriptionExists(String subscribee, String subscriber){
        return this.subscriptionRepository.findBySubscribeeUsernameAndSubscriberUsername(subscribee,subscriber);
    }

    public void saveSubscription(Subscription subscription) {
        subscriptionRepository.save(subscription);
    }

    public void deleteSubscription(Subscription subscription){
        this.subscriptionRepository.deleteById(subscription.getSubscriptionId());
    }

    public List<Subscription> findBySubscriberUsername(String subscriber) {
        return this.subscriptionRepository.findAllBySubscriberUsername(subscriber);
    }

    public List<Subscription> findBySubscribeeUsername(String subscribee) {
        return this.subscriptionRepository.findAllBySubscribeeUsername(subscribee);
    }
}
