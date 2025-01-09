package org.demo.huyminh.Service.Impl;

import jakarta.validation.constraints.Pattern;
import org.demo.huyminh.DTO.Request.VolunteerAppliUpdateRequest;
import org.demo.huyminh.Entity.User;
import org.demo.huyminh.Entity.VolunteerApplication;
import org.demo.huyminh.Repository.UserRepository;
import org.demo.huyminh.Repository.VolunteerRepository;
import org.demo.huyminh.Service.VolunteerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class VolunteerServiceImpl implements VolunteerService {

    @Autowired
    private VolunteerRepository volunteerRepository;
    @Autowired
    private UserRepository userRepository;

    //CreateApplicaiton
    @Override
    public VolunteerApplication createVolunteerAppli(String userId, String fullName
            , int yob, String gender, String address
            , @Pattern(regexp = "(84|0[3|5|7|8|9])+(\\d{8})\\b") String phone
            , String adoptionExp, String daysOfWeek, String morning,
                                                     String afternoon, String reason) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User Not Found"));

        VolunteerApplication application = new VolunteerApplication();

        application.setId(userId);
        application.setUser(user);
        application.setFullName(fullName);
        application.setYob(yob);
        application.setGender(gender);
        application.setAddress(address);
        application.setPhone(phone);
        application.setAdoptionExp(adoptionExp);
        application.setDaysOfWeek(daysOfWeek);
        application.setMorning(morning);
        application.setAfternoon(afternoon);
        application.setReason(reason);

        return volunteerRepository.save(application);
    }

    public List<VolunteerApplication> searchByFullName(String fullName) {
        return volunteerRepository.findByFullName(fullName);
    }

    //GetApplicationStatus = 0
    @Override
    public List<VolunteerApplication> getVolunteerApplications() {
        return volunteerRepository.findByStatusOrderByCreateAtAsc(0);
    }

    //GetApplicationStatus = 1
    @Override
    public List<VolunteerApplication> getVolunteerApplicationsWithStatus1() {
        return volunteerRepository.findByStatusOrderByUpdateAtDesc(1);
    }

    //GetApplicationStatus = 2
    @Override
    public List<VolunteerApplication> getVolunteerApplicationsWithStatus2() {
        return volunteerRepository.findByStatusOrderByUpdateAtDesc(2);
    }


    //Get All Volunteer Application
    public List<VolunteerApplication> getAllVolunteerApplications(){
        return volunteerRepository.findAll();
    }


    //GetApplicaitonById
    @Override
    public VolunteerApplication getVolunteerApplication(String volunteerAppliId) {
        return volunteerRepository.findById(volunteerAppliId)
                .orElseThrow(() -> new RuntimeException("Volunteer Application not founded"));
    }


    //UpdateApplicaitonById
    @Override
    public VolunteerApplication updateVolunApplication(String volunteerAppliId, VolunteerAppliUpdateRequest request) {
        VolunteerApplication application = volunteerRepository.findById(volunteerAppliId)
                .orElseThrow(() -> new RuntimeException("Application ID not found"));
        if (application.getStatus() == 0) {
            application.setFullName(request.getFullName());
            application.setYob(request.getYob());
            application.setGender(request.getGender());
            application.setAddress(request.getAddress());
            application.setPhone(request.getPhone());
            application.setAdoptionExp(request.getAdoptionExp());
            application.setDaysOfWeek(request.getDaysOfWeek());
            application.setMorning(request.getMorning());
            application.setAfternoon(request.getAfternoon());
            application.setReason(request.getReason());

            return volunteerRepository.save(application);
        } else {
            throw new RuntimeException("Can Not Update Volunteer Application");
        }
    }

    //Update Application Status
    @Override
    public VolunteerApplication updateAppilicationStatus(String volunteerAppliId, VolunteerAppliUpdateRequest request) {
        VolunteerApplication application = volunteerRepository.findById(volunteerAppliId)
                .orElseThrow(() -> new RuntimeException("Application Id Not Existed"));

        application.setStatus(request.getStatus());

        return volunteerRepository.save(application);

    }

    //DeleteApplicaitonById
    @Override
    public void deleteVolunApplication(String volunteerAppliId) {
        volunteerRepository.deleteById(volunteerAppliId);
    }

}
