package com.atguigu.gmall.api.service;


import com.atguigu.gmall.api.bean.UmsMember;
import com.atguigu.gmall.api.bean.UmsMemberReceiveAddress;

import java.util.List;

public interface UserService {

    List<UmsMember> getAllUser();

    List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId);

    UmsMember login(String username, String password);

    void addTokenRedis(String token, String id);

    UmsMember addOauthUser(UmsMember umsMember);

    UmsMember checkUmsMember(String uid);

    UmsMemberReceiveAddress getReceiveAddressById(String receiveAddressId);
}
