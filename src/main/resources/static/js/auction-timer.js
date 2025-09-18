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

