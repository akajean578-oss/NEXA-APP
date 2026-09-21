package com.nexa.controller;

import com.nexa.model.Friendship;
import com.nexa.model.Friendship.Status;
import com.nexa.model.User;
import com.nexa.repository.FriendshipRepository;
import com.nexa.repository.UserRepository;
import com.nexa.websocket.RealtimeNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/friends")
public class FriendshipController {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final RealtimeNotificationService realtime;

    public FriendshipController(FriendshipRepository friendshipRepository, UserRepository userRepository,
                                 RealtimeNotificationService realtime) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.realtime = realtime;
    }

    public record FriendDto(Long id, String fullName, String avatarUrl) {}
    public record RequestDto(Long friendshipId, Long fromUserId, String fromUserName) {}

    private User currentUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername()).orElseThrow();
    }

    /** Liste des amis (demandes acceptées) de l'utilisateur connecté. */
    @GetMapping
    public List<FriendDto> myFriends(@AuthenticationPrincipal UserDetails principal) {
        User me = currentUser(principal);
        return friendshipRepository.findAcceptedFor(me.getId()).stream()
                .map(f -> {
                    User other = f.getRequester().getId().equals(me.getId()) ? f.getAddressee() : f.getRequester();
                    return new FriendDto(other.getId(), other.getFullName(), other.getAvatarUrl());
                })
                .toList();
    }

    /** Demandes d'ami reçues, en attente. */
    @GetMapping("/requests")
    public List<RequestDto> pendingRequests(@AuthenticationPrincipal UserDetails principal) {
        User me = currentUser(principal);
        return friendshipRepository.findByAddresseeIdAndStatus(me.getId(), Status.PENDING).stream()
                .map(f -> new RequestDto(f.getId(), f.getRequester().getId(), f.getRequester().getFullName()))
                .toList();
    }

    @PostMapping("/request/{userId}")
    public ResponseEntity<?> sendRequest(@AuthenticationPrincipal UserDetails principal, @PathVariable Long userId) {
        User me = currentUser(principal);
        if (me.getId().equals(userId)) {
            return ResponseEntity.badRequest().body("Impossible de s'ajouter soi-même.");
        }
        if (friendshipRepository.findBetween(me.getId(), userId).isPresent()) {
            return ResponseEntity.badRequest().body("Une relation existe déjà avec cet utilisateur.");
        }
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur introuvable."));
        friendshipRepository.save(new Friendship(me, target));

        realtime.notifyUser(target.getEmail(), "friend_request", me.getFullName() + " vous a envoyé une demande d'ami.");
        return ResponseEntity.ok(Map.of("status", "sent"));
    }

    @PostMapping("/{friendshipId}/accept")
    public ResponseEntity<?> accept(@AuthenticationPrincipal UserDetails principal, @PathVariable Long friendshipId) {
        User me = currentUser(principal);
        Friendship f = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new NoSuchElementException("Demande introuvable."));
        if (!f.getAddressee().getId().equals(me.getId())) {
            return ResponseEntity.status(403).body("Vous ne pouvez pas répondre à cette demande.");
        }
        f.setStatus(Status.ACCEPTED);
        friendshipRepository.save(f);
        realtime.notifyUser(f.getRequester().getEmail(), "friend_accept", me.getFullName() + " a accepté votre demande d'ami.");
        return ResponseEntity.ok(Map.of("status", "accepted"));
    }

    @PostMapping("/{friendshipId}/decline")
    public ResponseEntity<?> decline(@AuthenticationPrincipal UserDetails principal, @PathVariable Long friendshipId) {
        User me = currentUser(principal);
        Friendship f = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new NoSuchElementException("Demande introuvable."));
        if (!f.getAddressee().getId().equals(me.getId())) {
            return ResponseEntity.status(403).body("Vous ne pouvez pas répondre à cette demande.");
        }
        f.setStatus(Status.DECLINED);
        friendshipRepository.save(f);
        return ResponseEntity.ok(Map.of("status", "declined"));
    }
}
