package com.adriens.personcounterapi;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class PersonCounterControllerTests {

    @Autowired
    private PersonCounterController controller;

    public void contextLoads(){
        assertThat(controller).isNotNull();
    }
}
