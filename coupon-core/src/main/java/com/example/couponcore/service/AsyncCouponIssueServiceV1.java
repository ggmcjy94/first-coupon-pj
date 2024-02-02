package com.example.couponcore.service;

import com.example.couponcore.component.DistributeLockExecutor;
import com.example.couponcore.exception.CouponIssueException;
import com.example.couponcore.repository.redis.RedisRepository;
import com.example.couponcore.repository.redis.dto.CouponIssueRequest;
import com.example.couponcore.repository.redis.dto.CouponRedisEntity;
import com.example.couponcore.util.CouponRedisUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.example.couponcore.exception.ErrorCode.FAIL_COUPON_ISSUE_REQUEST;

@RequiredArgsConstructor
@Service
public class AsyncCouponIssueServiceV1 {

    private final RedisRepository redisRepository;
    private final CouponIssueRedisService couponIssueRedisService;
    private final CouponCacheService couponCacheService;
    private final DistributeLockExecutor distributeLockExecutor;
    private final ObjectMapper objectMapper= new ObjectMapper();

//    public void issue(long couponId, long userId) {
//        String key = "issue.request.sorted_set.couponId=%s".formatted(couponId);
//        redisRepository.zAdd(key, String.valueOf(userId), System.currentTimeMillis());
//    }


    public void issue(long couponId, long userId) {
//        Coupon coupon = couponIssueService.findCoupon(couponId);
//        if (!coupon.availableIssueDate()) {
//            throw new CouponIssueException(INVALID_COUPON_ISSUE_DATE,
//                    "발급 가능한 일자가 아닙니다. couponId : %s, issueStart : %s, issueEnd : %s".formatted(couponId, coupon.getDateIssueStart(), coupon.getDateIssueEnd()));
//        }

        CouponRedisEntity coupon = couponCacheService.getCouponCache(couponId);
        coupon.checkIssuableCoupon();

        distributeLockExecutor.execute("lock_%s".formatted(couponId), 3000,3000,() -> {
            couponIssueRedisService.checkCouponIssueQuantity(coupon,userId);
            issueRequest(couponId,userId);
        });

    }

    private void issueRequest(long couponId , long userId) {
        CouponIssueRequest issueRequest = new CouponIssueRequest(couponId, userId);

        try {
            String value = objectMapper.writeValueAsString(issueRequest);
            redisRepository.sAdd(CouponRedisUtils.getIssueRequestKey(couponId), String.valueOf(userId));
            //queue 적재
            redisRepository.rPush(CouponRedisUtils.getIssueRequestQueueKey(),value);
        } catch (JsonProcessingException e) {
            throw new CouponIssueException(FAIL_COUPON_ISSUE_REQUEST, "input: %s".formatted(issueRequest));
        }

    }
}
