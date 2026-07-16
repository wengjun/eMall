package com.emall.user.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;

class UserControllerContractTest {
    @Test
    void exposesNoIndependentRegistrationStatusOrDeletionMutation() {
        assertThat(Arrays.stream(UserController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PostMapping.class))).isEmpty();
        assertThat(Arrays.stream(UserController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PatchMapping.class)).map(method -> method.getName()))
                .containsExactly("rename");
    }
}
