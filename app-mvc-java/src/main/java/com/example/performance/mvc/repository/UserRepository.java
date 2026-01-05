package com.example.performance.mvc.repository;

import com.example.performance.mvc.domain.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {
}
