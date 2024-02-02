package com.example.couponcore.service;

import com.example.couponcore.exception.CouponIssueException;
import com.example.couponcore.exception.ErrorCode;
import com.example.couponcore.model.Coupon;
import com.example.couponcore.model.CouponIssue;
import com.example.couponcore.model.event.CouponIssueCompleteEvent;
import com.example.couponcore.repository.mysql.CouponIssueRepository;
import com.example.couponcore.repository.mysql.CouponJpaRepository;
import com.example.couponcore.repository.mysql.CouponIssueJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class CouponIssueService {

    private final CouponIssueJpaRepository couponIssueJpaRepository;
    private final CouponIssueRepository couponIssueRepository;
    private final CouponJpaRepository couponJpaRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public void issue(long couponId, long userId) {
//        Coupon coupon = findCoupon(couponId);
        Coupon coupon = findCouponWithLock(couponId);
        coupon.issue();
        saveCouponIssue(couponId, userId);
        publishCouponEvent(coupon);
    }


    @Transactional
    public CouponIssue saveCouponIssue(long couponId, long userId) {
        checkAlreadyIssuance(couponId,userId);
        CouponIssue issue = CouponIssue.builder().couponId(couponId).userId(userId).build();
        return couponIssueJpaRepository.save(issue);
    }

    @Transactional(readOnly = true)
    public Coupon findCoupon(long couponId) {
        return couponJpaRepository.findById(couponId).orElseThrow( () -> {
            throw new CouponIssueException(ErrorCode.COUPON_NOT_EXIST,"쿠폰 정책이 존재하지 않습니다. %s".formatted(couponId));
        });
    }

    @Transactional(readOnly = true)
    public Coupon findCouponWithLock(long couponId) {
        return couponJpaRepository.findCouponWithLock(couponId).orElseThrow( () -> {
            throw new CouponIssueException(ErrorCode.COUPON_NOT_EXIST,"쿠폰 정책이 존재하지 않습니다. %s".formatted(couponId));
        });
    }

    private void checkAlreadyIssuance(long couponId, long userId) {
        CouponIssue issue = couponIssueRepository.findFirstCouponIssue(couponId, userId);
        if (issue != null) {
            throw new CouponIssueException(ErrorCode.DUPLICATED_COUPON_ISSUE,"이미 발급된 쿠폰입니다. user_id: %s , coupon_id: %s".formatted(couponId,userId));
        }


    }

    private void publishCouponEvent(Coupon coupon) {
        if (coupon.isIssueComplete()){
            applicationEventPublisher.publishEvent(new CouponIssueCompleteEvent(coupon.getId()));
        }
    }

}
