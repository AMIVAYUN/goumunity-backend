package com.ssafy.goumunity.domain.feed.service;

import com.ssafy.goumunity.domain.feed.controller.request.FeedImgRequest;
import com.ssafy.goumunity.domain.feed.controller.request.FeedRequest;
import com.ssafy.goumunity.domain.feed.controller.response.FeedRecommend;
import com.ssafy.goumunity.domain.feed.controller.response.FeedResponse;
import com.ssafy.goumunity.domain.feed.domain.Feed;
import com.ssafy.goumunity.domain.feed.domain.FeedImg;
import com.ssafy.goumunity.domain.feed.domain.FeedRecommendResource;
import com.ssafy.goumunity.domain.feed.domain.FeedWeight;
import com.ssafy.goumunity.domain.feed.exception.FeedErrorCode;
import com.ssafy.goumunity.domain.feed.exception.FeedException;
import com.ssafy.goumunity.domain.feed.service.post.FeedImageUploader;
import com.ssafy.goumunity.domain.feed.service.post.FeedImgRepository;
import com.ssafy.goumunity.domain.feed.service.post.FeedRepository;
import com.ssafy.goumunity.domain.user.domain.User;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedServiceImpl implements FeedService {

    private final FeedRepository feedRepository;
    private final FeedImgRepository feedImgRepository;
    private final FeedImageUploader feedImageUploader;

    private final CacheManager cacheManager;

    @Override
    @Transactional
    public Long createFeed(Long userId, FeedRequest.Create feedRequest, List<MultipartFile> images) {
        Feed createdFeed = feedRepository.create(Feed.create(feedRequest, userId));

        if (images != null && !images.isEmpty()) {
            for (int seq = 1; seq <= images.size(); seq++) {
                String savedUrl = feedImageUploader.uploadFeedImage(images.get(seq - 1));
                feedImgRepository.save(FeedImg.from(createdFeed.getId(), savedUrl, seq));
            }
        }
        return createdFeed.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedRecommend> findFeed(User user, Long regionId) {
        // 캐싱된 데이터가 완전 없으면 불러온다.
        if (cacheManager.getCache("recommends").get(user.getNickname()) == null) {
            findAllByRecommend(user, regionId);
        }

        int pageNumber = cacheManager.getCache("pagenumber").get(user.getNickname(), Integer.class);
        int maxPage = cacheManager.getCache("maxpage").get(user.getNickname(), Integer.class);
        int size = cacheManager.getCache("recommends").get(user.getNickname(), List.class).size();

        // 마지막페이지인 경우
        if (pageNumber == maxPage) {
            // 마지막페이지인데 게시글이 맞아떨어진 경우
            if (maxPage * 10 == size) findAllByRecommend(user, regionId);
            else {
                // 마지막페이지인데 게시글이 맞아떨어지지 않은 경우
                List<FeedRecommend> tempReturn =
                        cacheManager.getCache("recommends").get(user.getNickname(), List.class).stream()
                                .skip((maxPage - 1) * 10)
                                .limit(10)
                                .toList();
                findAllByRecommend(user, regionId);
                return tempReturn;
            }
        }

        cacheManager.getCache("pagenumber").put(user.getNickname(), pageNumber + 1);
        return cacheManager.getCache("recommends").get(user.getNickname(), List.class).stream()
                .skip((pageNumber - 1) * 10)
                .limit(10)
                .toList();
    }

    @Override
    public FeedResponse findOneFeed(Long userId, Long feedId) {
        return feedRepository.findOneFeed(userId, feedId);
    }

    @Override
    @Transactional
    public void modifyFeed(
            Long userId, Long feedId, FeedRequest.Modify feedRequest, List<MultipartFile> images) {
        Feed originalFeed =
                feedRepository
                        .findOneById(feedId)
                        .orElseThrow(() -> new FeedException(FeedErrorCode.FEED_NOT_FOUND));

        // 조회해온 feed의 작성자와 세션에 로그인 되어 있는 유저가 다르면 exception 발생
        originalFeed.checkUser(userId);

        // 기존 feed images 삭제
        List<FeedImg> originalFeedImages = feedImgRepository.findAllFeedImgByFeedId(feedId);
        for (FeedImg img : originalFeedImages) {
            feedImgRepository.delete(img.getId());
        }

        // feed 이미지 수정
        if (feedRequest.getFeedImages() == null && images != null) {
            // 기존에 있던 사진이 모두 없고 새로운 사진만 생겼을 경우
            for (int seq = 1; seq <= images.size(); seq++) {
                String savedUrl = feedImageUploader.uploadFeedImage(images.get(seq - 1));
                feedImgRepository.save(FeedImg.from(originalFeed.getId(), savedUrl, seq));
            }
        } else if (feedRequest.getFeedImages() != null && images == null) {
            // 새로운 사진은 없고 기존에 있던 사진에 대한 수정 사항만 있을 경우
            List<FeedImgRequest.Modify> feedImages = feedRequest.getFeedImages();
            feedImages.sort(Comparator.comparingInt(FeedImgRequest.Modify::getSequence));

            for (FeedImgRequest.Modify img : feedImages) {
                feedImgRepository.save(
                        FeedImg.from(originalFeed.getId(), img.getImgSrc(), img.getSequence()));
            }
        } else if (feedRequest.getFeedImages() != null && images != null) {
            // 기존에 있던 사진도 있고 새로운 사진도 있을 경우
            List<FeedImgRequest.Modify> feedImages = feedRequest.getFeedImages();
            feedImages.sort(Comparator.comparingInt(FeedImgRequest.Modify::getSequence));

            int nowIdx = 1;
            int newImageIdx = 0;
            for (int idx = 0; idx < feedImages.size(); ) {
                FeedImgRequest.Modify feedImage = feedImages.get(idx);
                if (feedImage.getSequence() == nowIdx) {
                    feedImgRepository.save(FeedImg.from(originalFeed.getId(), feedImage.getImgSrc(), nowIdx));
                    idx++;
                } else {
                    if (newImageIdx < images.size()) {
                        String savedUrl = feedImageUploader.uploadFeedImage(images.get(newImageIdx));
                        feedImgRepository.save(FeedImg.from(originalFeed.getId(), savedUrl, nowIdx));
                        newImageIdx++;
                    }
                }
                nowIdx++;
            }

            for (int idx = newImageIdx; idx < images.size(); idx++) {
                String savedUrl = feedImageUploader.uploadFeedImage(images.get(newImageIdx));
                feedImgRepository.save(FeedImg.from(originalFeed.getId(), savedUrl, nowIdx));
            }
        }

        // feed 내용 수정
        feedRepository.modify(Feed.create(originalFeed, feedRequest));
    }

    @Override
    @Transactional
    public void deleteFeed(Long userId, Long feedId) {
        Feed originalFeed =
                feedRepository
                        .findOneById(feedId)
                        .orElseThrow(() -> new FeedException(FeedErrorCode.FEED_NOT_FOUND));

        // 조회해온 feed의 작성자와 세션에 로그인 되어 있는 유저가 다르면 exception 발생
        originalFeed.checkUser(userId);

        feedRepository.delete(originalFeed.getId());
    }

    private void findAllByRecommend(User user, Long regionId) {
        List<FeedRecommendResource> feeds = feedRepository.findFeed(user.getId(), regionId);
        List<FeedWeight> feedWeights =
                feeds.stream().map(item -> FeedWeight.from(item, user)).sorted().toList();
        List<FeedRecommend> recommends =
                feedWeights.stream()
                        .map(item -> FeedRecommend.from(item.getFeedRecommendResource()))
                        .limit(200)
                        .toList();

        int maxPage = recommends.size() / 10;
        if (recommends.size() % 10 != 0) maxPage++;

        cacheManager.getCache("recommends").put(user.getNickname(), recommends);
        cacheManager.getCache("pagenumber").put(user.getNickname(), 1);
        cacheManager.getCache("maxpage").put(user.getNickname(), maxPage);
    }
}
