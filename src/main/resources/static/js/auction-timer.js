/**
 * Auction Timer JavaScript
 * Handles countdown timers for active auctions with real-time updates
 */

class AuctionTimer {
  constructor(auctionId, displayElement) {
    this.auctionId = auctionId;
    this.displayElement = displayElement;
    this.intervalId = null;
    this.timeRemaining = 0;
    this.isActive = false;
    this.extensionDuration = 30;

    this.init();
  }

  init() {
    this.fetchAuctionData();
    // Update every 5 seconds
    this.intervalId = setInterval(() => this.updateTimer(), 5000);
  }

  async fetchAuctionData() {
    try {
      const response = await fetch(`/api/auctions/${this.auctionId}/timer`);
      if (response.ok) {
        const data = await response.json();
        this.timeRemaining = data.timeRemaining || 0;
        this.isActive = data.isActive || false;
        this.extensionDuration = data.extensionDuration || 30;
        this.updateDisplay();
      }
    } catch (error) {
      console.error("Error fetching auction data:", error);
      this.displayElement.innerHTML =
        '<span class="text-danger">Error loading timer</span>';
    }
  }

  updateTimer() {
    if (this.isActive && this.timeRemaining > 0) {
      this.timeRemaining -= 5; // Subtract 5 seconds (update interval)
      this.updateDisplay();

      if (this.timeRemaining <= 0) {
        this.timeRemaining = 0;
        this.isActive = false;
        this.updateDisplay();
        this.onAuctionEnd();
      }
    }
  }

  updateDisplay() {
    if (!this.isActive) {
      this.displayElement.innerHTML =
        '<span class="text-muted">Auction Ended</span>';
      return;
    }

    if (this.timeRemaining <= 0) {
      this.displayElement.innerHTML =
        '<span class="text-danger">Auction Ended</span>';
      return;
    }

    const hours = Math.floor(this.timeRemaining / 3600);
    const minutes = Math.floor((this.timeRemaining % 3600) / 60);
    const seconds = this.timeRemaining % 60;

    let timeString = "";
    if (hours > 0) {
      timeString += `${hours}h `;
    }
    if (minutes > 0 || hours > 0) {
      timeString += `${minutes}m `;
    }
    timeString += `${seconds}s`;

    // Color coding based on time remaining - update parent timer class
    const timerElement = this.displayElement.closest(".countdown-timer");
    if (timerElement) {
      // Remove existing urgency classes
      timerElement.classList.remove("urgent", "critical");

      if (this.timeRemaining <= 60) {
        timerElement.classList.add("critical");
      } else if (this.timeRemaining <= 300) {
        // 5 minutes
        timerElement.classList.add("urgent");
      }
    }

    this.displayElement.innerHTML = `<span>${timeString}</span>`;
  }

  onAuctionEnd() {
    // Notify parent components that auction has ended
    this.displayElement.dispatchEvent(
      new CustomEvent("auctionEnded", {
        detail: { auctionId: this.auctionId },
      })
    );
  }

  destroy() {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }
}

// Global function to initialize auction timer
function initializeAuctionTimer(auctionId, displayElement) {
  return new AuctionTimer(auctionId, displayElement);
}

// WebSocket integration for real-time updates
class AuctionWebSocket {
  constructor() {
    this.socket = null;
    this.stompClient = null;
    this.connected = false;
    this.timers = new Map();
  }

  connect() {
    if (typeof SockJS === "undefined" || typeof Stomp === "undefined") {
      console.warn(
        "SockJS or Stomp not available. WebSocket features disabled."
      );
      return;
    }

    this.socket = new SockJS("/ws");
    this.stompClient = Stomp.over(this.socket);

    this.stompClient.connect(
      {},
      (frame) => {
        console.log("Connected to WebSocket:", frame);
        this.connected = true;
        this.subscribeToUpdates();
      },
      (error) => {
        console.error("WebSocket connection error:", error);
        this.connected = false;
      }
    );
  }

  subscribeToUpdates() {
    if (!this.stompClient) return;

    // Subscribe to general auction updates
    this.stompClient.subscribe("/topic/auctions", (message) => {
      const auction = JSON.parse(message.body);
      this.handleAuctionUpdate(auction);
    });

    // Subscribe to specific auction updates
    this.timers.forEach((timer, auctionId) => {
      this.stompClient.subscribe(`/topic/auctions/${auctionId}`, (message) => {
        const auction = JSON.parse(message.body);
        this.handleAuctionUpdate(auction);
      });
    });
  }

  handleAuctionUpdate(auction) {
    const timer = this.timers.get(auction.auctionId);
    if (timer) {
      timer.timeRemaining = auction.timeRemaining || 0;
      timer.isActive = auction.status === "ACTIVE";
      timer.updateDisplay();
    }
  }

  registerTimer(auctionId, timer) {
    this.timers.set(auctionId, timer);

    if (this.connected && this.stompClient) {
      this.stompClient.subscribe(`/topic/auctions/${auctionId}`, (message) => {
        const auction = JSON.parse(message.body);
        this.handleAuctionUpdate(auction);
      });
    }
  }

  disconnect() {
    if (this.stompClient) {
      this.stompClient.disconnect();
    }
    this.connected = false;
  }
}

// Global WebSocket instance
const auctionWebSocket = new AuctionWebSocket();

// Initialize WebSocket connection when page loads
document.addEventListener("DOMContentLoaded", function () {
  auctionWebSocket.connect();
});

// Enhanced timer initialization with WebSocket support
function initializeAuctionTimerWithWebSocket(auctionId, displayElement) {
  const timer = new AuctionTimer(auctionId, displayElement);
  auctionWebSocket.registerTimer(auctionId, timer);
  return timer;
}

// Utility functions
function formatTimeRemaining(seconds) {
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const secs = seconds % 60;

  if (hours > 0) {
    return `${hours}:${minutes.toString().padStart(2, "0")}:${secs
      .toString()
      .padStart(2, "0")}`;
  } else {
    return `${minutes}:${secs.toString().padStart(2, "0")}`;
  }
}

function getTimeRemainingClass(seconds) {
  if (seconds <= 60) return "critical";
  if (seconds <= 300) return "urgent";
  return "";
}

// Export for module systems
if (typeof module !== "undefined" && module.exports) {
  module.exports = {
    AuctionTimer,
    AuctionWebSocket,
    initializeAuctionTimer,
    initializeAuctionTimerWithWebSocket,
    formatTimeRemaining,
    getTimeRemainingClass,
  };
}
