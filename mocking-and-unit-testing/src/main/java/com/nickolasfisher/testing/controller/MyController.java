package com.nickolasfisher.testing.controller;

import com.nickolasfisher.testing.dto.DownstreamResponseDTO;
import com.nickolasfisher.testing.dto.PersonDTO;
import com.nickolasfisher.testing.service.MyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.function.Function;

@RestController
public class MyController {

    private final MyService service;

    public MyController(MyService service) {
        this.service = service;
    }

    @GetMapping("/persons")
    public Flux<PersonDTO> getPersons() {
        return service.getAllPeople().map(downstreamResponseDTO -> {
            PersonDTO personDTO = new PersonDTO();

            personDTO.setFirstName(downstreamResponseDTO.getFirstName());
            personDTO.setLastName(downstreamResponseDTO.getLastName());

            return personDTO;
        });
    }
}
