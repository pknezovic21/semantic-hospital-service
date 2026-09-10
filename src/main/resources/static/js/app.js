const $ = selector => document.querySelector(selector);
const message = $("#message");

const fields = {
    search: $("#search"),
    status: $("#status"),
    type: $("#type"),
    unit: $("#unit")
};

let management;
let organization = [];
let availableEquipment = [];
let equipmentRequests = [];
let purchaseRequests = [];
let refreshPage = async () => {};

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function display(value) {
    return value ? escapeHtml(value) : "-";
}

function date(value) {
    return value ? new Date(value).toLocaleString() : "-";
}

function showError(error) {
    message.textContent = error.message;
    message.hidden = false;
    window.scrollTo(0, 0);
}

async function getJson(url, options) {
    const response = await fetch(url, options);
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.message || `Request failed (${response.status}).`);
    }
    return response.status === 204 ? null : response.json();
}

function setOptions(select, items, placeholder) {
    select.innerHTML = `<option value="">${placeholder}</option>` + items.map(item =>
        `<option value="${escapeHtml(item.id)}">${escapeHtml(item.name)}</option>`
    ).join("");
}

function empty(columns, text) {
    return `<tr><td colspan="${columns}" class="empty">${text}</td></tr>`;
}

function unitItems() {
    return organization.filter(unit => unit.type !== "Hospital");
}

function isWithinUnit(childId, parentId) {
    let current = childId;
    while (current) {
        if (current === parentId) return true;
        current = organization.find(unit => unit.id === current)?.parentId;
    }
    return false;
}

function locationsForUnit(unitId) {
    return management.locations.filter(location => isWithinUnit(location.servesUnitId, unitId));
}

async function loadReferences() {
    [management, organization] = await Promise.all([
        getJson("/api/equipment-management"),
        getJson("/api/organization")
    ]);
    if (fields.status) {
        setOptions(fields.status, management.equipmentStatuses, "All statuses");
        setOptions(fields.type, management.equipmentTypes, "All types");
        setOptions(fields.unit, unitItems().filter(unit => unit.type === "Department"), "All departments");
    }
    if ($("#request-unit")) {
        setOptions($("#request-unit"), unitItems(), "Select unit");
        setOptions($("#request-type"), management.equipmentTypes, "Select type");
    }
}

async function loadEquipment() {
    const parameters = new URLSearchParams();
    Object.entries(fields).forEach(([name, field]) => {
        if (field.value.trim()) parameters.set(name, field.value.trim());
    });
    const equipment = await getJson(`/api/equipment?${parameters}`);
    $("#result-count").textContent = `${equipment.length} item(s)`;
    $("#equipment-rows").innerHTML = equipment.length ? equipment.map(item => `
        <tr data-id="${escapeHtml(item.id)}">
            <td>${display(item.assetNumber)}</td><td>${display(item.name)}</td>
            <td>${display(item.equipmentTypeName)}</td><td>${display(item.status)}</td>
            <td>${display(item.assignedUnitName)}</td><td>${display(item.locationName)}</td>
        </tr>`).join("") : empty(6, "No equipment found.");
}

async function loadDetail(id) {
    const item = await getJson(`/api/equipment/${encodeURIComponent(id)}`);
    const locations = locationsForUnit(item.assignedUnitId);
    $("#equipment-detail").innerHTML = `
        <h2>${display(item.name)}</h2>
        <dl class="detail-list">
            <dt>Asset number</dt><dd>${display(item.assetNumber)}</dd>
            <dt>Status</dt><dd>${display(item.status)}</dd>
            <dt>Type</dt><dd>${display(item.equipmentTypeName)}</dd>
            <dt>Category</dt><dd>${display(item.categoryName)}</dd>
            <dt>Unit</dt><dd>${display(item.assignedUnitName)}</dd>
            <dt>Location</dt><dd>${display(item.locationName)}</dd>
            <dt>Supplier</dt><dd>${display(item.supplierName)}</dd>
            <dt>Contract</dt><dd>${display(item.maintenanceContractName)}</dd>
            <dt>Maintenance</dt><dd>${item.maintenanceHistory.length}</dd>
            <dt>Loans</dt><dd>${item.loanHistory.length}</dd>
            <dt>High risk</dt><dd class="${item.highRisk ? "risk" : ""}">${item.highRisk ? "Yes" : "No"}</dd>
        </dl>
        <form data-location-form="${escapeHtml(item.id)}">
            <label><span>Change location</span><select required>${locations.map(location =>
                `<option value="${escapeHtml(location.id)}" ${location.id === item.locationId ? "selected" : ""}>${escapeHtml(location.name)}</option>`
            ).join("")}</select></label>
            <button type="submit">Update</button>
        </form>`;
}

async function loadRequests() {
    equipmentRequests = await getJson("/api/requests");
    $("#request-rows").innerHTML = equipmentRequests.length ? equipmentRequests.map(item => {
        const candidates = item.candidates.map(candidate =>
            `${candidate.equipmentName} (${candidate.assetNumber}, ${candidate.assignedUnitName}, ${candidate.locationName})`
        ).join("; ");
        const result = [item.highPriority ? "High priority" : "", candidates || (item.purchaseNeeded ? "Purchase needed" : "")]
            .filter(Boolean).join("; ") || "-";
        const cancel = item.status === "Open"
            ? `<button data-cancel-request="${escapeHtml(item.id)}">Cancel</button>` : "";
        return `<tr><td>${display(item.requestNumber)}</td><td>${display(item.requestedForUnitName)}</td>
            <td>${display(item.requestedTypeName)}</td><td>${display(item.reason)}</td>
            <td>${display(item.status)}</td><td>${escapeHtml(result)}</td><td>${cancel}</td></tr>`;
    }).join("") : empty(7, "No requests found.");
}

async function loadLoans() {
    const loans = await getJson("/api/loans");
    $("#loan-rows").innerHTML = loans.length ? loans.map(loan => {
        const action = loan.returnedAt ? "" : `<button data-return-loan="${escapeHtml(loan.id)}">Return</button>`;
        return `<tr><td>${display(loan.loanNumber)}</td><td>${display(loan.equipmentName)} (${display(loan.assetNumber)})</td>
            <td>${display(loan.loanedFromUnitName)}</td><td>${display(loan.loanedToUnitName)} / ${display(loan.loanedToLocationName)}</td>
            <td>${date(loan.loanedAt)}</td><td>${date(loan.returnedAt)}</td><td>${action}</td></tr>`;
    }).join("") : empty(7, "No loans found.");
}

async function loadPurchases() {
    purchaseRequests = await getJson("/api/purchase-requests");
    $("#purchase-rows").innerHTML = purchaseRequests.length ? purchaseRequests.map(item => {
        const cancel = item.status === "Pending"
            ? `<button data-cancel-purchase="${escapeHtml(item.id)}">Cancel</button>` : "";
        const received = item.receivedEquipmentName
            ? `${item.receivedEquipmentName} (${item.receivedAssetNumber})` : "-";
        return `<tr><td>${display(item.purchaseNumber)}</td><td>${display(item.equipmentRequestId)}</td>
            <td>${display(item.requestedForUnitName)}</td><td>${display(item.requestedTypeName)}</td>
            <td>${display(item.supplierName)}</td><td>${display(item.status)}</td><td>${escapeHtml(received)}</td><td>${cancel}</td></tr>`;
    }).join("") : empty(8, "No purchase requests found.");
}

async function loadMaintenance() {
    const records = await getJson("/api/maintenance");
    $("#maintenance-rows").innerHTML = records.length ? records.map(record => {
        const action = record.completedAt ? ""
            : `<button data-complete-maintenance="${escapeHtml(record.id)}">Complete</button>`;
        return `<tr><td>${display(record.maintenanceNumber)}</td><td>${display(record.equipmentName)} (${display(record.assetNumber)})</td>
            <td>${display(record.reportedForUnitName)}</td><td>${display(record.reason)}</td>
            <td>${date(record.reportedAt)}</td><td>${date(record.completedAt)}</td>
            <td>${record.highPriority ? "High" : "Normal"}</td><td>${action}</td></tr>`;
    }).join("") : empty(8, "No maintenance records found.");
}

async function loadOverview() {
    const [overview, report, risk, units] = await Promise.all([
        getJson("/api/hospital/overview"), getJson("/api/reports/equipment"),
        getJson("/api/equipment/risk"), getJson("/api/organization")
    ]);
    const metrics = {
        "Organization components": overview.organizationUnitCount,
        "Equipment": overview.equipmentCount,
        "Available equipment": overview.availableEquipmentCount,
        "In maintenance": overview.inMaintenanceEquipmentCount,
        "Loaned equipment": overview.loanedEquipmentCount,
        "Open requests": overview.openRequestCount,
        "Active loans": overview.activeLoanCount,
        "Purchase requests": overview.purchaseRequestCount,
        "Maintenance records": overview.maintenanceRecordCount
    };
    $("#overview-rows").innerHTML = Object.entries(metrics).map(([name, count]) =>
        `<tr><td>${name}</td><td>${count}</td></tr>`).join("");
    $("#organization-rows").innerHTML = units.map(unit => {
        const parent = units.find(item => item.id === unit.parentId)?.name;
        return `<tr><td>${display(unit.name)}</td><td>${display(unit.type)}</td>
            <td>${display(parent)}</td><td>${unit.equipmentShortage ? "Yes" : "No"}</td></tr>`;
    }).join("");
    const groups = {Status: report.byStatus, Type: report.byType, Category: report.byCategory, Unit: report.byUnit};
    $("#report-rows").innerHTML = Object.entries(groups).flatMap(([group, items]) =>
        items.map(item => `<tr><td>${group}</td><td>${display(item.name)}</td><td>${item.count}</td></tr>`)
    ).join("");
    $("#risk-rows").innerHTML = risk.length ? risk.map(item =>
        `<tr><td>${display(item.assetNumber)}</td><td>${display(item.name)}</td><td>${display(item.equipmentType)}</td>
            <td>${display(item.assignedUnitName)}</td><td>${item.maintenanceRecordCount}</td></tr>`
    ).join("") : empty(5, "No high-risk equipment found.");
}

function updateLoanForm() {
    const request = equipmentRequests.find(item => item.id === $("#loan-request").value);
    if (request) {
        setOptions($("#loan-equipment"), request.candidates.map(candidate => ({
            id: candidate.equipmentId, name: `${candidate.equipmentName} (${candidate.assetNumber})`
        })), "Select candidate");
        setOptions($("#loan-unit"), [{id: request.requestedForUnitId, name: request.requestedForUnitName}], "Select unit");
    } else {
        setOptions($("#loan-equipment"), availableEquipment.map(item => ({id: item.id, name: `${item.name} (${item.assetNumber})`})), "Select equipment");
        setOptions($("#loan-unit"), unitItems(), "Select unit");
    }
    updateLoanLocations();
}

function updateLoanUnits() {
    if ($("#loan-request").value) return;
    const equipment = availableEquipment.find(item => item.id === $("#loan-equipment").value);
    const units = equipment ? unitItems().filter(unit => unit.id !== equipment.assignedUnitId) : unitItems();
    setOptions($("#loan-unit"), units, "Select unit");
    updateLoanLocations();
}

function updateLoanLocations() {
    setOptions($("#loan-location"), locationsForUnit($("#loan-unit").value), "Select location");
}

function updatePurchaseSupplier() {
    const request = equipmentRequests.find(item => item.id === $("#purchase-request").value);
    const suppliers = request ? management.suppliers.filter(supplier =>
        supplier.supportedTypeIds.includes(request.requestedTypeId)) : [];
    setOptions($("#purchase-supplier"), suppliers, "Select supplier");
}

function updateReceiveLocations() {
    const purchase = purchaseRequests.find(item => item.id === $("#receive-purchase").value);
    setOptions($("#receive-location"), purchase ? locationsForUnit(purchase.requestedForUnitId) : [], "Select location");
}

function updateRequestForms() {
    setOptions($("#loan-request"), equipmentRequests.filter(item => item.status === "Open" && item.candidates.length)
        .map(item => ({id: item.id, name: `${item.requestNumber} - ${item.requestedTypeName}`})), "No request");
    setOptions($("#purchase-request"), equipmentRequests.filter(item => item.status === "Open" && item.purchaseNeeded)
        .map(item => ({id: item.id, name: `${item.requestNumber} - ${item.requestedTypeName}`})), "Select request");
    setOptions($("#receive-purchase"), purchaseRequests.filter(item => item.status === "Pending")
        .map(item => ({id: item.id, name: `${item.purchaseNumber} - ${item.requestedTypeName}`})), "Select purchase");
    updateLoanForm();
    updatePurchaseSupplier();
    updateReceiveLocations();
}

async function refreshRequests() {
    availableEquipment = await getJson("/api/equipment?status=Available");
    await Promise.all([loadRequests(), loadLoans(), loadPurchases()]);
    updateRequestForms();
}

async function refreshMaintenance() {
    availableEquipment = await getJson("/api/equipment?status=Available");
    await loadMaintenance();
    setOptions($("#maintenance-equipment"), availableEquipment.map(item => ({
        id: item.id, name: `${item.name} (${item.assetNumber})`
    })), "Select equipment");
}

async function save(url, method, body) {
    const options = {method};
    if (body) {
        options.headers = {"Content-Type": "application/json"};
        options.body = JSON.stringify(body);
    }
    await getJson(url, options);
    message.hidden = true;
    await refreshPage();
}

$("#filters")?.addEventListener("submit", event => {
    event.preventDefault();
    loadEquipment().catch(showError);
});

$("#clear-filters")?.addEventListener("click", () => {
    $("#filters").reset();
    loadEquipment().catch(showError);
});

$("#equipment-rows")?.addEventListener("click", event => {
    const row = event.target.closest("tr[data-id]");
    if (row) loadDetail(row.dataset.id).catch(showError);
});

$("#equipment-detail")?.addEventListener("submit", event => {
    event.preventDefault();
    const form = event.target.closest("form[data-location-form]");
    save(`/api/equipment/${encodeURIComponent(form.dataset.locationForm)}/location`, "PATCH", {
        locationId: form.querySelector("select").value
    }).then(() => loadDetail(form.dataset.locationForm)).catch(showError);
});

$("#request-form")?.addEventListener("submit", event => {
    event.preventDefault();
    save("/api/requests", "POST", {
        requestedForUnitId: $("#request-unit").value,
        requestedTypeId: $("#request-type").value,
        reason: $("#request-reason").value
    }).then(() => event.target.reset()).catch(showError);
});

$("#request-rows")?.addEventListener("click", event => {
    const button = event.target.closest("button[data-cancel-request]");
    if (button) save(`/api/requests/${encodeURIComponent(button.dataset.cancelRequest)}/cancel`, "PATCH").catch(showError);
});

$("#loan-request")?.addEventListener("change", updateLoanForm);
$("#loan-equipment")?.addEventListener("change", updateLoanUnits);
$("#loan-unit")?.addEventListener("change", updateLoanLocations);
$("#loan-form")?.addEventListener("submit", event => {
    event.preventDefault();
    save("/api/loans", "POST", {
        requestId: $("#loan-request").value || null,
        equipmentId: $("#loan-equipment").value,
        loanedToUnitId: $("#loan-unit").value,
        loanedToLocationId: $("#loan-location").value
    }).then(() => event.target.reset()).catch(showError);
});

$("#loan-rows")?.addEventListener("click", event => {
    const button = event.target.closest("button[data-return-loan]");
    if (button) save(`/api/loans/${encodeURIComponent(button.dataset.returnLoan)}/return`, "PATCH").catch(showError);
});

$("#purchase-request")?.addEventListener("change", updatePurchaseSupplier);
$("#purchase-form")?.addEventListener("submit", event => {
    event.preventDefault();
    save("/api/purchase-requests", "POST", {
        equipmentRequestId: $("#purchase-request").value,
        supplierId: $("#purchase-supplier").value,
        reason: $("#purchase-reason").value
    }).then(() => event.target.reset()).catch(showError);
});

$("#receive-purchase")?.addEventListener("change", updateReceiveLocations);
$("#receive-form")?.addEventListener("submit", event => {
    event.preventDefault();
    save(`/api/purchase-requests/${encodeURIComponent($("#receive-purchase").value)}/receive`, "POST", {
        locationId: $("#receive-location").value
    }).then(() => event.target.reset()).catch(showError);
});

$("#purchase-rows")?.addEventListener("click", event => {
    const button = event.target.closest("button[data-cancel-purchase]");
    if (button) save(`/api/purchase-requests/${encodeURIComponent(button.dataset.cancelPurchase)}/cancel`, "PATCH").catch(showError);
});

$("#maintenance-form")?.addEventListener("submit", event => {
    event.preventDefault();
    save("/api/maintenance", "POST", {
        equipmentId: $("#maintenance-equipment").value,
        reason: $("#maintenance-reason").value
    }).then(() => event.target.reset()).catch(showError);
});

$("#maintenance-rows")?.addEventListener("click", event => {
    const button = event.target.closest("button[data-complete-maintenance]");
    if (button) save(`/api/maintenance/${encodeURIComponent(button.dataset.completeMaintenance)}/complete`, "PATCH").catch(showError);
});

async function initialize() {
    switch (document.body.dataset.page) {
        case "equipment":
            await loadReferences();
            refreshPage = loadEquipment;
            break;
        case "requests":
            await loadReferences();
            refreshPage = refreshRequests;
            break;
        case "maintenance":
            refreshPage = refreshMaintenance;
            break;
        default:
            refreshPage = loadOverview;
    }
    await refreshPage();
}

initialize().catch(showError);
