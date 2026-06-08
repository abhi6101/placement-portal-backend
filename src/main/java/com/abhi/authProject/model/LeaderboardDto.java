package com.abhi.authProject.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardDto {
    private int id;
    private String username;
    private String name;
    private int points;
}
