package com.openisle.service;

import com.openisle.model.CampusPersonType;

public record WhuCasProfile(
  String subject,
  String campusId,
  String displayName,
  String email,
  CampusPersonType personType,
  String department
) {}
