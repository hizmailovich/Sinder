package com.solvd.laba.sinder.service.impl;

import com.solvd.laba.sinder.domain.parties.Party;
import com.solvd.laba.sinder.domain.exception.IllegalActionException;
import com.solvd.laba.sinder.domain.exception.ResourceNotFoundException;
import com.solvd.laba.sinder.domain.parties.PartyMatch;
import com.solvd.laba.sinder.domain.parties.PartyMatchStatus;
import com.solvd.laba.sinder.domain.User;
import com.solvd.laba.sinder.persistence.PartyMatchRepository;
import com.solvd.laba.sinder.service.PartyMatchService;
import com.solvd.laba.sinder.service.PartyService;
import com.solvd.laba.sinder.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PartyMatchServiceImpl implements PartyMatchService {

    private final PartyMatchRepository partyMatchRepository;
    private final PartyService partyService;
    private final UserService userService;

    @Override
    @Transactional(readOnly = true)
    public PartyMatch retrieveById(Long partyMatchId) {
        return partyMatchRepository.findById(partyMatchId)
                .orElseThrow(() -> new ResourceNotFoundException("PartyMatch with id = " + partyMatchId + " not found!"));
    }

    @Override
    @Transactional(readOnly = true)
    public PartyMatch retrieveByGuestIdAndPartyId(Long guestId, Long partyId) {
        return partyMatchRepository.findByGuestIdAndPartyId(guestId, partyId)
                .orElseThrow(() -> new ResourceNotFoundException("PartyMatch with guest's id = " + guestId + " and party's id = " + partyId + " not found!"));
    }

    private Boolean isExist(Long guestId, Long partyId) {
        return partyMatchRepository.existsByGuestIdAndPartyId(guestId, partyId);
    }

    @Override
    @Transactional
    public PartyMatch create(PartyMatch partyMatch) {
        return partyMatchRepository.save(partyMatch);
    }

    @Override
    @Transactional
    public PartyMatch update(PartyMatch partyMatch) {
        PartyMatch foundPartyMatch = retrieveById(partyMatch.getId());
        foundPartyMatch.setStatus(partyMatch.getStatus());
        return partyMatchRepository.save(partyMatch);
    }

    @Override
    @Transactional
    public PartyMatch requestParty(Long guestId, Long partyId) {
        User guest = userService.retrieveById(guestId);
        Party party = partyService.retrieveById(partyId);
        if (isExist(guestId, partyId)) {
            PartyMatch partyMatch = retrieveByGuestIdAndPartyId(guestId, partyId);
            if (!partyMatch.getStatus().equals(PartyMatchStatus.INVITED)) {
                throw new IllegalActionException("Party with id = " + partyId + " already interacted with you, and didn't invite you!");

            }
            partyMatch.setStatus(PartyMatchStatus.APPROVED);
            return update(partyMatch);
        }
        PartyMatch partyMatch = PartyMatch.builder()
                .status(PartyMatchStatus.REQUESTED)
                .guest(guest)
                .party(party)
                .build();
        return create(partyMatch);
    }

    @Override
    @Transactional
    public PartyMatch skipParty(Long guestId, Long partyId) {
        User guest = userService.retrieveById(guestId);
        Party party = partyService.retrieveById(partyId);
        if (isExist(guestId, partyId)) {
            PartyMatch partyMatch = retrieveByGuestIdAndPartyId(guestId, partyId);
            if (!partyMatch.getStatus().equals(PartyMatchStatus.INVITED)) {
                throw new IllegalActionException("Party with id = " + partyId + " already interacted with you, and didn't invite you!");
            }
            partyMatch.setStatus(PartyMatchStatus.REJECTED);
            return update(partyMatch);
        }
        PartyMatch partyMatch = PartyMatch.builder()
                .status(PartyMatchStatus.REJECTED)
                .guest(guest)
                .party(party)
                .build();
        return create(partyMatch);
    }

    @Override
    @Transactional
    public PartyMatch inviteGuest(Long partyId, Long guestId) {
        User guest = userService.retrieveById(guestId);
        Party party = partyService.retrieveById(partyId);
        if (isExist(guestId, partyId)) {
            PartyMatch partyMatch = retrieveByGuestIdAndPartyId(guestId, partyId);
            if (!partyMatch.getStatus().equals(PartyMatchStatus.REQUESTED)) {
                throw new IllegalActionException("You can't approve guest, who didn't request an invitation!");
            }
            partyMatch.setStatus(PartyMatchStatus.APPROVED);
            return update(partyMatch);
        }
        PartyMatch partyMatch = PartyMatch.builder()
                .status(PartyMatchStatus.INVITED)
                .guest(guest)
                .party(party)
                .build();
        return create(partyMatch);
    }

    @Override
    @Transactional
    public PartyMatch skipGuest(Long partyId, Long guestId) {
        User guest = userService.retrieveById(guestId);
        Party party = partyService.retrieveById(partyId);
        if (isExist(guestId, partyId)) {
            PartyMatch partyMatch = retrieveByGuestIdAndPartyId(guestId, partyId);
            if (!partyMatch.getStatus().equals(PartyMatchStatus.REQUESTED)) {
                throw new IllegalActionException("You can't skip guest, who didn't request an invitation!");
            }
            partyMatch.setStatus(PartyMatchStatus.REJECTED);
            return update(partyMatch);
        }
        PartyMatch partyMatch = PartyMatch.builder()
                .status(PartyMatchStatus.REJECTED)
                .guest(guest)
                .party(party)
                .build();
        return create(partyMatch);
    }

}
