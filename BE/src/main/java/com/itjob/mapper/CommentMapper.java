package com.itjob.mapper;

import com.itjob.dto.response.CommentResponse;
import com.itjob.entity.Comment;
import com.itjob.mapper.config.CentralMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CentralMapperConfig.class, uses = {UserMapper.class})
public interface CommentMapper {

    @Mapping(target = "user", source = "author")
    CommentResponse toCommentResponse(Comment comment);
}
