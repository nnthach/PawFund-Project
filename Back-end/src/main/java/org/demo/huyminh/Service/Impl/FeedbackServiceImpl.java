package org.demo.huyminh.Service.Impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.demo.huyminh.DTO.Reponse.BriefFeedbackResponse;
import org.demo.huyminh.DTO.Reponse.FeedbackResponse;
import org.demo.huyminh.DTO.Reponse.PetFeedbackResponse;
import org.demo.huyminh.DTO.Request.FeedbackCreationRequest;
import org.demo.huyminh.Entity.*;
import org.demo.huyminh.Enums.Status;
import org.demo.huyminh.Exception.AppException;
import org.demo.huyminh.Exception.ErrorCode;
import org.demo.huyminh.Mapper.FeedbackMapper;
import org.demo.huyminh.Mapper.UserMapper;
import org.demo.huyminh.Repository.*;
import org.demo.huyminh.Service.FeedbackService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Minh
 * Date: 10/17/2024
 * Time: 5:54 PM
 */

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class FeedbackServiceImpl implements FeedbackService {

    FeedbackRepository feedbackRepository;
    TaskRepository taskRepository;
    RatingRepository ratingRepository;
    FeedbackMapper feedbackMapper;
    UserMapper userMapper;
    PetRepository petRepository;
    ApplicationRepository applicationRepository;

    @Override
    public FeedbackResponse createFeedback(int taskId, FeedbackCreationRequest request, User reporter) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        if (!task.getTeam().contains(reporter)) {
            throw new AppException(ErrorCode.USER_NOT_IN_TEAM);
        }

        if (task.getStatus().equals(Status.NOT_STARTED)) {
            throw new AppException(ErrorCode.CANNOT_CREATE_FEEDBACK);
        }

        log.info("Request: {}", request);

        Feedback feedback = Feedback.builder()
                .content(request.getContent())
                .task(task)
                .reporter(reporter)
                .build();

        Rating rating = request.getRating();
        if (rating != null) {
            rating.setFeedback(feedback);
            if (task.getCategory().equals("Adoption")) {
                rating.setApplication(applicationRepository.getApplicationByTaskId(task.getId()));
            }
            feedback.setRating(rating);
        }

        List<Image> images = request.getImages().stream()
                .map(imageUrl -> Image.builder()
                        .feedback(feedback)
                        .imageUrl(imageUrl)
                        .build())
                .toList();

        feedback.setImages(images);
        task.getFeedbacks().add(feedback);

        feedbackRepository.save(feedback);

        log.info("New feedback created for task {}: {}", taskId, feedback);

        FeedbackResponse response = feedbackMapper.toFeedbackResponse(feedback);
        response.setReporter(userMapper.toUserResponse(reporter));
        response.setImages(feedback.getImages().stream().map(Image::getImageUrl).toList());
        response.setRating(feedback.getRating());

        return response;
    }

    @Override
    public List<PetFeedbackResponse> getAvailablePets() {
        List<Pet> pets = ratingRepository.findAvailablePets();
        return pets.stream()
                .map(pet -> PetFeedbackResponse.builder()
                        .petId(pet.getPetId())
                        .petName(pet.getPetName())
                        .petStatus(pet.getPetStatus())
                        .feedbackCount(ratingRepository.countDistinctPetsWithFeedback(pet.getPetName()))
                        .build())
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public List<FeedbackResponse> getPotentialAdopters(double minRating) {

        return ratingRepository.findByAverageRatingGreaterThan(minRating)
                .stream()
                .map(rating -> feedbackMapper.toFeedbackResponse(rating.getFeedback()))
                .toList();
    }

    @Override
    public List<BriefFeedbackResponse> getHighRatingApplication(String petName, String sortBy, String sortDir, User user) {
        if (sortBy == null || !sortBy.equalsIgnoreCase("rating")) {
            sortBy = "RATING";
        }
        if (sortDir == null || (!sortDir.equalsIgnoreCase("asc") && !sortDir.equalsIgnoreCase("desc"))) {
            sortDir = "DESC";
        }

        List<BriefFeedbackResponse> responses = new ArrayList<>();
        if (petName == null || petName.isEmpty()) {
            List<Rating> ratings = ratingRepository.findTopRatings(null, sortBy.toUpperCase(), sortDir.toUpperCase());
            responses = ratings.stream()
                    .map(newRating -> {
                        return BriefFeedbackResponse.builder()
                                .taskId(newRating.getFeedback().getTask().getId())
                                .adopterName(newRating.getApplication().getUser().getUsername())
                                .rating(newRating.getAverageRating())
                                .applicationId(newRating.getApplication().getApplicationId())
                                .feedbackFinishedAt(newRating.getFeedback().getEditedAt())
                                .taskCreatedAt(newRating.getFeedback().getTask().getCreatedAt())
                                .build();
                    })
                    .toList();
        } else {
            Pet existingPet = petRepository.findByPetName(petName)
                    .orElseThrow(() -> new AppException(ErrorCode.PET_NOT_FOUND));
            List<Rating> ratings = ratingRepository.findTopRatings(existingPet.getPetId(), sortBy.toUpperCase(), sortDir.toUpperCase());
            responses = ratings.stream()
                    .map(newRating -> {
                        return BriefFeedbackResponse.builder()
                                .taskId(newRating.getFeedback().getTask().getId())
                                .adopterName(newRating.getApplication().getUser().getUsername())
                                .rating(newRating.getAverageRating())
                                .applicationId(newRating.getApplication().getApplicationId())
                                .feedbackFinishedAt(newRating.getFeedback().getEditedAt())
                                .taskCreatedAt(newRating.getFeedback().getTask().getCreatedAt())
                                .build();
                    })
                    .toList();
        }
        return responses;
    }


    @Override
    public List<FeedbackResponse> getFeedbacksByTaskId(int taskId) {

        taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        List<Feedback> feedbacks = feedbackRepository.findAllByTaskId(taskId);
        List<FeedbackResponse> responses = feedbacks.stream().map(feedbackMapper::toFeedbackResponse).toList();
        for (FeedbackResponse response : responses) {
            response.setImages(feedbacks.getFirst().getImages().stream().map(Image::getImageUrl).toList());
            response.setRating(feedbacks.getFirst().getRating());
        }
        return responses;
    }

    @Transactional
    @Override
    public void updateFeedback(int feedbackId, int taskId, FeedbackCreationRequest request, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));
        Feedback existingFeedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_NOT_FOUND));

        if (!existingFeedback.getTask().equals(task)) {
            throw new AppException(ErrorCode.FEEDBACK_NOT_BELONG_TO_TASK);
        }
        if (!existingFeedback.getReporter().equals(user) && !task.getOwner().equals(user)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_TO_UPDATE_FEEDBACK);
        }

        existingFeedback.setContent(request.getContent());

        Rating rating = request.getRating();
        if (rating != null) {
            if (existingFeedback.getRating() != null) {
                Rating existingRating = existingFeedback.getRating();
                existingRating.setFamilyIncome(rating.getFamilyIncome());
                existingRating.setFamilyStability(rating.getFamilyStability());
                existingRating.setLivingSpace(rating.getLivingSpace());
                existingRating.setPetExperience(rating.getPetExperience());
                existingRating.setTimeCommitment(rating.getTimeCommitment());
            } else {
                rating.setFeedback(existingFeedback);
                existingFeedback.setRating(rating);
            }
        }

        existingFeedback.getImages().clear();

        List<Image> newImages = request.getImages().stream()
                .map(imageUrl -> Image.builder()
                        .feedback(existingFeedback)
                        .imageUrl(imageUrl)
                        .build())
                .toList();
        existingFeedback.getImages().addAll(newImages);

        existingFeedback.setEditedAt(LocalDateTime.now());

        feedbackRepository.save(existingFeedback);
    }

    @Override
    public void deleteFeedback(int feedbackId, int taskId, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_NOT_FOUND));

        if (!feedback.getTask().equals(task)) {
            throw new AppException(ErrorCode.FEEDBACK_NOT_BELONG_TO_TASK);
        }

        if (!feedback.getReporter().equals(user) && !task.getOwner().equals(user)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_TO_DELETE_FEEDBACK);
        }

        task.getFeedbacks().remove(feedback);
        taskRepository.save(task);
        feedbackRepository.delete(feedback);
    }
}