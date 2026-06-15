// Toast notification functions
function showToast(type, message) {
  const toastElement = document.getElementById(type + "Toast");
  const toastBody = document.getElementById(type + "ToastBody");
  toastBody.textContent = message;

  const toast = new bootstrap.Toast(toastElement);
  toast.show();
}

// View notification details in modal
function viewNotification(notificationId) {
  console.log("Viewing notification:", notificationId);

  // Find the notification data from the page
  const notificationCard = document
    .querySelector(`[onclick*="viewNotification(${notificationId})"]`)
    .closest(".card");
  const notificationData = extractNotificationData(notificationCard);

  console.log("Extracted data:", notificationData);

  // Populate modal with notification details
  populateNotificationModal(notificationData);

  // Mark as read when viewing
  markAsRead(notificationId, false);
}

// Extract notification data from the card
function extractNotificationData(card) {
  try {
    console.log("Extracting data from card:", card);

    // Get notification content
    const contentElement = card.querySelector("p.mb-2");
    const content = contentElement
      ? contentElement.textContent.trim()
      : "No content available";

    // Get notification type - look for the badge that contains the type (not the "New" badge)
    let type = "Unknown";
    const badgeElements = card.querySelectorAll(".badge");
    for (let badge of badgeElements) {
      const text = badge.textContent.trim();
      // Skip the "New" badge and look for type badges
      if (
        text !== "New" &&
        (text === "WINNER" ||
          text === "OUTBID" ||
          text === "CLOSURE" ||
          text === "BID_PLACED" ||
          text === "AUCTION_STARTED" ||
          text === "AUCTION_ENDING")
      ) {
        type = text;
        break;
      }
    }

    // Get sent time - look for the first small.text-muted element (timestamp)
    const timeElement = card.querySelector("small.text-muted");
    const sentAt = timeElement
      ? timeElement.textContent.trim()
      : "Unknown time";

    // Get auction ID - look for "Auction #" text in all small elements
    let auctionId = "Unknown";
    const allSmallElements = card.querySelectorAll("small");
    for (let element of allSmallElements) {
      const text = element.textContent;
      const match = text.match(/Auction #(\d+)/);
      if (match) {
        auctionId = match[1];
        break;
      }
    }

    // Get vehicle info - look for text after car icon
    let vehicle = "Unknown vehicle";
    const carIcon = card.querySelector(".bi-car-front");
    if (carIcon && carIcon.nextElementSibling) {
      vehicle = carIcon.nextElementSibling.textContent.trim();
    }

    // Check if email was sent
    const emailSent = card.querySelector(".bi-envelope-check") !== null;

    console.log("Extracted data:", {
      content,
      type,
      sentAt,
      auctionId,
      vehicle,
      emailSent,
    });

    // Debug: Log all badges found
    const allBadges = card.querySelectorAll(".badge");
    console.log(
      "All badges found:",
      Array.from(allBadges).map((b) => b.textContent.trim())
    );

    return {
      content: content,
      type: type,
      sentAt: sentAt,
      auctionId: auctionId,
      vehicle: vehicle,
      emailSent: emailSent,
    };
  } catch (error) {
    console.error("Error extracting notification data:", error);
    return {
      content: "Error loading notification details",
      type: "Error",
      sentAt: "Unknown",
      auctionId: "Unknown",
      vehicle: "Unknown",
      emailSent: false,
    };
  }
}

// Populate the notification modal
function populateNotificationModal(data) {
  const modalBody = document.getElementById("notificationDetails");
  const goToAuctionBtn = document.getElementById("goToAuctionBtn");

  console.log("Populating modal with data:", data);

  modalBody.innerHTML = `
    <div class="notification-details">
      <!-- Header with notification type -->
      <div class="d-flex align-items-center mb-4">
        <div class="notification-icon me-3">
          <i class="bi bi-bell-fill text-primary" style="font-size: 2rem;"></i>
        </div>
        <div>
          <h5 class="mb-1">${
            data.type === "WINNER"
              ? "Winner"
              : data.type === "OUTBID"
              ? "Outbid"
              : data.type === "CLOSURE"
              ? "Auction Closed"
              : data.type === "BID_PLACED"
              ? "Bid Placed"
              : data.type === "AUCTION_STARTED"
              ? "Auction Started"
              : data.type === "AUCTION_ENDING"
              ? "Auction Ending"
              : data.type
          } Notification</h5>
          <small class="text-muted">${data.sentAt}</small>
        </div>
      </div>

      <!-- Main content -->
      <div class="notification-content mb-4">
        <div class="alert alert-info border-0 shadow-sm">
          <div class="d-flex align-items-start">
            <i class="bi bi-chat-dots me-3 mt-1 text-info"></i>
            <div>
              <h6 class="alert-heading mb-2">Message</h6>
              <p class="mb-0">${data.content}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Details grid -->
      <div class="row g-3">
        <div class="col-md-6">
          <div class="card border-0 bg-light">
            <div class="card-body p-3">
              <h6 class="card-title text-primary mb-2">
                <i class="bi bi-hammer me-2"></i>Auction Information
              </h6>
              <p class="card-text mb-1">
                <strong>Auction ID:</strong> #${data.auctionId}
              </p>
              <p class="card-text mb-0">
                <strong>Vehicle:</strong> ${data.vehicle}
              </p>
            </div>
          </div>
        </div>
        
        <div class="col-md-6">
          <div class="card border-0 bg-light">
            <div class="card-body p-3">
              <h6 class="card-title text-success mb-2">
                <i class="bi bi-envelope me-2"></i>Delivery Status
              </h6>
              <p class="card-text mb-1">
                <strong>Email:</strong> 
                <span class="badge ${
                  data.emailSent ? "bg-success" : "bg-warning"
                }">
                  ${data.emailSent ? "Sent" : "Not Sent"}
                </span>
              </p>
              <p class="card-text mb-0">
                <strong>Status:</strong> 
                <span class="badge bg-info">Delivered</span>
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- Action buttons -->
      <div class="mt-4 text-center">
        <div class="btn-group" role="group">
          <button type="button" class="btn btn-outline-primary" onclick="copyNotificationContent()">
            <i class="bi bi-clipboard me-2"></i>Copy Content
          </button>
          <button type="button" class="btn btn-outline-secondary" onclick="shareNotification()">
            <i class="bi bi-share me-2"></i>Share
          </button>
        </div>
      </div>
    </div>
  `;

  // Update the Go to Auction button
  if (data.auctionId && data.auctionId !== "Unknown") {
    goToAuctionBtn.href = `/auctions/details/${data.auctionId}`;
    goToAuctionBtn.style.display = "inline-block";
  } else {
    goToAuctionBtn.style.display = "none";
  }
}

// Additional utility functions
function copyNotificationContent() {
  const content = document.querySelector(".notification-content p").textContent;
  navigator.clipboard
    .writeText(content)
    .then(() => {
      showToast("success", "Content copied to clipboard!");
    })
    .catch(() => {
      showToast("error", "Failed to copy content");
    });
}

function shareNotification() {
  if (navigator.share) {
    const content = document.querySelector(
      ".notification-content p"
    ).textContent;
    navigator.share({
      title: "Notification from Vehicle Bidding System",
      text: content,
    });
  } else {
    showToast("info", "Sharing not supported on this device");
  }
}

function markAsRead(notificationId, showToast = true) {
  fetch(`/notifications/${notificationId}/mark-read`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
  })
    .then((response) => response.json())
    .then((data) => {
      if (data.success) {
        if (showToast) {
          showToast("success", "Notification marked as read");
          setTimeout(() => location.reload(), 1000);
        } else {
          // Update UI without reload
          updateNotificationUI(notificationId);
        }
      } else {
        showToast("error", "Error: " + data.error);
      }
    })
    .catch((error) => {
      console.error("Error:", error);
      showToast(
        "error",
        "An error occurred while marking notification as read"
      );
    });
}

function updateNotificationUI(notificationId) {
  const notificationCard = document
    .querySelector(`[onclick*="viewNotification(${notificationId})"]`)
    .closest(".card");
  const markReadBtn = notificationCard.querySelector('[onclick*="markAsRead"]');
  const newBadge = notificationCard.querySelector(".badge.bg-primary");

  // Remove "New" badge
  if (newBadge) {
    newBadge.remove();
  }

  // Remove mark as read button
  if (markReadBtn) {
    markReadBtn.remove();
  }

  // Update card background
  notificationCard.querySelector(".card-body").classList.remove("bg-white");
  notificationCard.querySelector(".card-body").classList.add("bg-light");
}

function markAllAsRead() {
  fetch("/notifications/mark-all-read", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
  })
    .then((response) => response.json())
    .then((data) => {
      if (data.success) {
        showToast("success", "All notifications marked as read");
        setTimeout(() => location.reload(), 1000);
      } else {
        showToast("error", "Error: " + data.error);
      }
    })
    .catch((error) => {
      console.error("Error:", error);
      showToast(
        "error",
        "An error occurred while marking all notifications as read"
      );
    });
}

function deleteNotification(notificationId) {
  // Create a custom confirmation modal
  const confirmModal = `
    <div class="modal fade" id="confirmDeleteModal" tabindex="-1">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">Confirm Delete</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
          </div>
          <div class="modal-body">
            Are you sure you want to delete this notification? This action cannot be undone.
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
            <button type="button" class="btn btn-danger" onclick="confirmDelete(${notificationId})">Delete</button>
          </div>
        </div>
      </div>
    </div>
  `;

  // Remove existing modal if any
  const existingModal = document.getElementById("confirmDeleteModal");
  if (existingModal) {
    existingModal.remove();
  }

  // Add modal to page
  document.body.insertAdjacentHTML("beforeend", confirmModal);

  // Show modal
  const modal = new bootstrap.Modal(
    document.getElementById("confirmDeleteModal")
  );
  modal.show();
}

function confirmDelete(notificationId) {
  fetch(`/notifications/${notificationId}`, {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
    },
  })
    .then((response) => response.json())
    .then((data) => {
      if (data.success) {
        showToast("success", "Notification deleted successfully");
        setTimeout(() => location.reload(), 1000);
      } else {
        showToast("error", "Error: " + data.error);
      }
    })
    .catch((error) => {
      console.error("Error:", error);
      showToast("error", "An error occurred while deleting notification");
    });
}

function refreshNotifications() {
  showToast("success", "Refreshing notifications...");
  setTimeout(() => location.reload(), 500);
}

// Search functionality
document.addEventListener("DOMContentLoaded", function () {
  const searchInput = document.getElementById("searchNotifications");
  if (searchInput) {
    searchInput.addEventListener("input", function (e) {
      const searchTerm = e.target.value.toLowerCase();
      const notificationCards = document.querySelectorAll(".card.mb-3");

      notificationCards.forEach((card) => {
        const content = card.textContent.toLowerCase();
        if (content.includes(searchTerm)) {
          card.style.display = "block";
        } else {
          card.style.display = "none";
        }
      });
    });
  }
});

// Auto-refresh unread count every 30 seconds
setInterval(function () {
  fetch("/api/notifications/unread-count")
    .then((response) => response.json())
    .then((data) => {
      const badge = document.querySelector(".badge.bg-danger");
      if (badge) {
        badge.textContent = data.unreadCount;
      }
    })
    .catch((error) => console.error("Error fetching unread count:", error));
}, 30000);
