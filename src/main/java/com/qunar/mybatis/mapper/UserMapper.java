package com.qunar.mybatis.mapper;

import com.qunar.mybatis.domain.User;

import java.util.List;

/**
 * Created by wolfcode-lanxw
 */
public interface UserMapper {
    int insert(User user);
    List<User> selectAll();
}
