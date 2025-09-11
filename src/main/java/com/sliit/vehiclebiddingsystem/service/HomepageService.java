package com.sliit.vehiclebiddingsystem.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.sliit.vehiclebiddingsystem.dto.HomepageAuctionDto;
import com.sliit.vehiclebiddingsystem.dto.HomepageStatsDto;
import com.sliit.vehiclebiddingsystem.entity.Auction;
import com.sliit.vehiclebiddingsystem.entity.Bid;
import com.sliit.vehiclebiddingsystem.entity.VehicleImage;
import com.sliit.vehiclebiddingsystem.entity.VehicleListing;
import com.sliit.vehiclebiddingsystem.repository.AuctionRepository;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;
import com.sliit.vehiclebiddingsystem.repository.VehicleListingRepository;

/**
 * Service for homepage data operations
 */
@Service
public class HomepageService {

    private static final Logger logger = LoggerFactory.getLogger(HomepageService.class);

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private VehicleListingRepository listingRepository;

    @Autowired
    private UserRepository userRepository;


}
