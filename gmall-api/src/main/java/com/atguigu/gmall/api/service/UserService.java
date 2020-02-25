package com.atguigu.gmall.api.service;


import com.atguigu.gmall.api.bean.UmsMember;
import com.atguigu.gmall.api.bean.UmsMemberReceiveAddress;

import java.util.List;

public interface UserService {

    List<UmsMember> getAllUser();

    List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId);
}
