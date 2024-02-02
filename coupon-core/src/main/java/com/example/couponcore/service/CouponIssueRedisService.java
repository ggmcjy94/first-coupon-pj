package com.example.couponcore.service;

import com.example.couponcore.exception.CouponIssueException;
import com.example.couponcore.repository.redis.RedisRepository;
import com.example.couponcore.repository.redis.dto.CouponRedisEntity;
import com.example.couponcore.util.CouponRedisUtils;
import jodd.util.CollectionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.example.couponcore.exception.ErrorCode.DUPLICATED_COUPON_ISSUE;
import static com.example.couponcore.exception.ErrorCode.INVALID_COUPON_ISSUE_QUANTITY;

@RequiredArgsConstructor
@Service
public class CouponIssueRedisService {
    private final RedisRepository redisRepository;
    public void checkCouponIssueQuantity(CouponRedisEntity couponRedisEntity, long userId) {
        if (!availableUserIssueQuantity(couponRedisEntity.id(),userId)){
            throw new CouponIssueException(DUPLICATED_COUPON_ISSUE,
                    "이미 발급 요청이 처리되었습니다. couponId : %s, userId: %s".formatted(couponRedisEntity.id(), userId));
        }

        if (!availableTotalIssueQuantity(couponRedisEntity.totalQuantity(), couponRedisEntity.id())) {
            throw new CouponIssueException(INVALID_COUPON_ISSUE_QUANTITY,
                    "발급 가능한 수량을 초과합니다. couponId : %s, userId : %s".formatted(couponRedisEntity.id(), userId));
        }


    }

    public boolean availableTotalIssueQuantity(Integer totalQuantity, long couponId) {
        if (totalQuantity == null) {
            return true;
        }

        String key = CouponRedisUtils.getIssueRequestKey(couponId);
        return totalQuantity > redisRepository.sCard(key);
    }

    public boolean availableUserIssueQuantity(long couponId, long userId) {
        String key = CouponRedisUtils.getIssueRequestKey(couponId);
        return !redisRepository.sIsMember(key, String.valueOf(userId));
    }
}
