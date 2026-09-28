(() => {
  "use strict";

  const form = document.querySelector("#transform-form");
  const action = document.querySelector("#action");
  const endpointsGroup = document.querySelector("#endpoints-group");
  const endpoints = document.querySelector("#endpoints");
  const file = document.querySelector("#file");
  const result = document.querySelector("#result");
  const healthButton = document.querySelector("#health-button");
  const healthStatus = document.querySelector("#health-status");

  function setStatus(element, message, isError) {
    element.textContent = message;
    element.classList.toggle("error", Boolean(isError));
    element.classList.toggle("success", !isError);
  }

  function updateEndpointVisibility() {
    const needsEndpoints = action.value === "clear-endpoints" || action.value === "keep-endpoints";
    endpointsGroup.hidden = !needsEndpoints;
    endpoints.required = needsEndpoints;
  }

  action.addEventListener("change", updateEndpointVisibility);
  updateEndpointVisibility();

  healthButton.addEventListener("click", async () => {
    healthButton.disabled = true;
    setStatus(healthStatus, "Vérification en cours…", false);
    try {
      const response = await fetch("/health");
      const body = await response.json();
      if (!response.ok) throw new Error(body.message || "Le serveur est indisponible.");
      setStatus(healthStatus, `Serveur disponible (${body.status}).`, false);
    } catch (error) {
      setStatus(healthStatus, error.message || "Le serveur est indisponible.", true);
    } finally {
      healthButton.disabled = false;
    }
  });

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!file.files.length) {
      setStatus(result, "Sélectionnez un fichier.", true);
      return;
    }

    const query = new URLSearchParams({ extension: document.querySelector("#extension").value });
    if (endpointsGroup.hidden === false) query.set("endpoints", endpoints.value.trim());
    const body = new FormData();
    body.append("file", file.files[0]);
    setStatus(result, "Transformation en cours…", false);

    try {
      const response = await fetch(`/${action.value}?${query}`, { method: "POST", body });
      const contentType = response.headers.get("content-type") || "";
      if (!response.ok || contentType.includes("application/json")) {
        const error = await response.json();
        throw new Error(error.message || "La transformation a échoué.");
      }
      const blob = await response.blob();
      const disposition = response.headers.get("content-disposition") || "";
      const filename = disposition.match(/filename="([^"]+)"/i)?.[1] || `${action.value}.zip`;
      const link = document.createElement("a");
      link.href = URL.createObjectURL(blob);
      link.download = filename;
      link.click();
      URL.revokeObjectURL(link.href);
      setStatus(result, "Transformation terminée : le ZIP a été téléchargé.", false);
    } catch (error) {
      setStatus(result, error.message || "La transformation a échoué.", true);
    }
  });
})();
