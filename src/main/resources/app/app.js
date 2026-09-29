(() => {
  "use strict";

  const form = document.querySelector("#transform-form");
  const action = document.querySelector("#action");
  const endpointsGroup = document.querySelector("#endpoints-group");
  const endpoints = document.querySelector("#endpoints");
  const archiveName = document.querySelector("#archive-name");
  const file = document.querySelector("#file");
  const preserveComments = document.querySelector("#preserve-comments");
  const result = document.querySelector("#result");
  const documentation = document.querySelector(".documentation");
  const transformationCard = document.querySelector(".transformation-card");

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

  function synchronizeDocumentationHeight() {
    if (!documentation.open || window.matchMedia("(max-width: 650px)").matches) {
      documentation.style.height = "";
      return;
    }
    documentation.style.height = `${transformationCard.offsetHeight}px`;
  }

  action.addEventListener("change", () => {
    updateEndpointVisibility();
    synchronizeDocumentationHeight();
  });
  documentation.addEventListener("toggle", synchronizeDocumentationHeight);
  window.addEventListener("resize", synchronizeDocumentationHeight);
  new ResizeObserver(synchronizeDocumentationHeight).observe(transformationCard);
  updateEndpointVisibility();
  synchronizeDocumentationHeight();

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!file.files.length) {
      setStatus(result, "Sélectionnez un fichier.", true);
      return;
    }

    const query = new URLSearchParams({
      extension: document.querySelector("#extension").value,
      "preserve-comments": String(preserveComments.checked),
      "archive-name": archiveName.value.trim()
    });
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
