const form = document.getElementById("upload-form");
const fileInput = document.getElementById("audio-file");
const submitButton = document.getElementById("submit-button");
const statusMessage = document.getElementById("status-message");
const results = document.getElementById("results");

form.addEventListener("submit", async (event) => {
  event.preventDefault();

  const file = fileInput.files[0];
  if (!file) {
    setStatus("Select an audio file before starting the screening.", "error");
    return;
  }

  const data = new FormData();
  data.append("file", file);

  setStatus("Analyzing breathing sound and generating report...", "idle");
  submitButton.disabled = true;

  try {
    const response = await fetch("/api/analyze", {
      method: "POST",
      body: data,
    });

    const payload = await response.json();
    if (!response.ok) {
      throw new Error(payload.detail || "Screening failed.");
    }

    renderResults(payload);
    setStatus(`Finished screening ${payload.file_name}.`, "success");
  } catch (error) {
    setStatus(error.message, "error");
    results.className = "results empty";
    results.innerHTML = "<p>We could not analyze that recording yet.</p>";
  } finally {
    submitButton.disabled = false;
  }
});

function setStatus(message, type) {
  statusMessage.textContent = message;
  statusMessage.className = `status ${type}`;
}

function renderResults(payload) {
  const featureSummary = payload.feature_summary;
  const explanationItems = payload.explanation
    .map(
      (item) =>
        `<li><strong>${escapeHtml(item.segment)}</strong> (${item.start_seconds}s - ${item.end_seconds}s): ${escapeHtml(item.reason)}</li>`
    )
    .join("");

  const recommendationItems = payload.recommendations.map((item) => `<li>${escapeHtml(item)}</li>`).join("");
  const differentialText = payload.differentials.map((item) => escapeHtml(item)).join(", ");

  results.className = "results";
  results.innerHTML = `
    <div class="result-layout">
      <div class="result-card">
        <h3>${escapeHtml(payload.predicted_condition)}</h3>
        <p>${escapeHtml(payload.model_used)}</p>
        <div class="metric-row">
          <div class="metric">
            <span class="metric-label">Confidence</span>
            <span class="metric-value">${Math.round(payload.confidence * 100)}%</span>
          </div>
          <div class="metric">
            <span class="metric-label">Severity</span>
            <span class="metric-value">${payload.severity}</span>
          </div>
          <div class="metric">
            <span class="metric-label">Duration</span>
            <span class="metric-value">${featureSummary.duration_seconds}s</span>
          </div>
          <div class="metric">
            <span class="metric-label">Sample rate</span>
            <span class="metric-value">${featureSummary.sample_rate} Hz</span>
          </div>
        </div>
        <span class="${payload.urgent_alert ? "urgent-pill" : "safe-pill"}">
          ${payload.urgent_alert ? "Urgent case flag raised" : "No urgent flag raised"}
        </span>
        <h3>Recommendations</h3>
        <ul class="plain-list">${recommendationItems}</ul>
        <p><strong>Differentials to review:</strong> ${differentialText}</p>
      </div>

      <div class="explanation-card">
        <h3>Explainability Regions</h3>
        <ul class="plain-list">${explanationItems}</ul>
        <h3>Feature Snapshot</h3>
        <ul class="plain-list">
          <li><strong>Backend:</strong> ${escapeHtml(featureSummary.extraction_backend)}</li>
          <li><strong>Spectral centroid:</strong> ${featureSummary.spectral_centroid}</li>
          <li><strong>Zero crossing rate:</strong> ${featureSummary.zero_crossing_rate}</li>
          <li><strong>RMS energy:</strong> ${featureSummary.rms_energy}</li>
        </ul>
      </div>
    </div>
    <div class="report-card">
      <h3>Diagnostic Report</h3>
      <pre>${escapeHtml(payload.report)}</pre>
    </div>
  `;
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}
