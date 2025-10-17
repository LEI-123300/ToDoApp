package com.example.examplefeature.ui;

import com.example.pdf.PdfGenerator;
import com.example.base.ui.component.ViewToolbar;
import com.example.examplefeature.Task;
import com.example.examplefeature.TaskService;
import com.example.qrcode.QRCodeUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;


import com.vaadin.flow.component.html.Label;
import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

@SuppressWarnings("removal")
@Route("")
@PageTitle("Task List")
@Menu(order = 0, icon = "vaadin:clipboard-check", title = "Task List")
class TaskListView extends Main {

    private final TaskService taskService;

    final TextField description;
    final DatePicker dueDate;
    final Button createBtn;
    final Button exportPdfBtn;
    final Grid<Task> taskGrid;

    TaskListView(TaskService taskService) {
        this.taskService = taskService;

        description = new TextField();
        description.setPlaceholder("What do you want to do?");
        description.setAriaLabel("Task description");
        description.setMaxLength(Task.DESCRIPTION_MAX_LENGTH);
        description.setMinWidth("20em");

        dueDate = new DatePicker();
        dueDate.setPlaceholder("Due date");
        dueDate.setAriaLabel("Due date");

        createBtn = new Button("Create", event -> createTask());
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        exportPdfBtn = new Button("Exportar PDF", event -> exportTasksToPdf());
        exportPdfBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        Button currencyBtn = new Button("Câmbio de Moedas", e -> openCurrencyDialog());
        currencyBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);


        var dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(getLocale())
                .withZone(ZoneId.systemDefault());
        var dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(getLocale());

        taskGrid = new Grid<>();
        taskGrid.setItems(query -> taskService.list(toSpringPageRequest(query)).stream());
        taskGrid.addColumn(Task::getDescription).setHeader("Description");
        taskGrid.addColumn(task -> Optional.ofNullable(task.getDueDate()).map(dateFormatter::format).orElse("Never"))
                .setHeader("Due Date");
        taskGrid.addColumn(task -> dateTimeFormatter.format(task.getCreationDate())).setHeader("Creation Date");
        // --- QR CODE COLUMN ---
        taskGrid.addColumn(new ComponentRenderer<>(task -> {
            Button qrBtn = new Button(new Icon(VaadinIcon.QRCODE));
            qrBtn.getElement().setProperty("title", "Pesquisar tarefa no Google");

            qrBtn.addClickListener(e -> {
                try {
                    // Cria o link da pesquisa Google
                    String queryUrl = "https://www.google.com/search?q=" +
                            URLEncoder.encode(task.getDescription(), StandardCharsets.UTF_8.name());

                    // Gera o QRCode
                    byte[] png = QRCodeUtil.generateQRCodePng(queryUrl, 300, 300);

                    // Usa StreamResource (ainda funciona, mas está "deprecated")
                    com.vaadin.flow.server.StreamResource sr =
                            new com.vaadin.flow.server.StreamResource("qrcode.png", () -> new ByteArrayInputStream(png));

                    Image qrImage = new Image(sr, "QR Code");
                    qrImage.setWidth("300px");
                    qrImage.setHeight("300px");

                    Dialog dialog = new Dialog();
                    dialog.add(new VerticalLayout(qrImage));
                    dialog.setWidth("350px");
                    dialog.setHeight("380px");
                    dialog.open();

                } catch (Exception ex) {
                    Notification.show("Erro ao gerar QR code: " + ex.getMessage(),
                            3000, Notification.Position.MIDDLE);
                }
            });




            return qrBtn;
        })).setHeader("QR");
        taskGrid.setSizeFull();

        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(new ViewToolbar("Task List",
                ViewToolbar.group(description, dueDate, createBtn, exportPdfBtn, currencyBtn)));
        add(taskGrid);
    }

    private void createTask() {
        taskService.createTask(description.getValue(), dueDate.getValue());
        taskGrid.getDataProvider().refreshAll();
        description.clear();
        dueDate.clear();
        Notification.show("Task added", 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void exportTasksToPdf() {
        try {
            // Recolhe as descrições das tarefas
            List<String> tasks = Optional.ofNullable(taskService.findAllTasks())
                    .orElse(List.of())
                    .stream()
                    .map(Task::getDescription)
                    .filter(description -> !description.isBlank())
                    .toList();

            // Gera o PDF
            byte[] pdfBytes = PdfGenerator.generateTasksPdfBytes(tasks, "Lista de Tarefas");

            // Cria o recurso do PDF
            com.vaadin.flow.server.StreamResource resource =
                    new com.vaadin.flow.server.StreamResource("tarefas.pdf",
                            () -> new ByteArrayInputStream(pdfBytes));
            resource.setContentType("application/pdf");

            // Cria o link de download invisível
            Anchor downloadLink = new Anchor(resource, "");
            downloadLink.getElement().setAttribute("download", true);
            downloadLink.getStyle().set("display", "none");

            // Adiciona o link temporariamente ao layout e dispara o clique no browser
            getElement().appendChild(downloadLink.getElement());
            downloadLink.getElement().executeJs("this.click(); $0.remove();", downloadLink.getElement());

            Notification.show("PDF gerado com sucesso!", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        } catch (Exception e) {
            Notification.show("Erro ao gerar PDF: " + e.getMessage(),
                            5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    private void openCurrencyDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Conversor de Moedas");

        TextField fromField = new TextField("De (ex: EUR)");
        TextField toField = new TextField("Para (ex: USD)");
        NumberField amountField = new NumberField("Valor");
        amountField.setPlaceholder("Ex: 100");
        amountField.setWidth("100%");

        Button convertBtn = new Button("Converter");
        convertBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Label resultLabel = new Label();

        convertBtn.addClickListener(e -> {
            try {
                String from = fromField.getValue().trim();
                String to = toField.getValue().trim();
                Double amount = amountField.getValue();

                if (from.isEmpty() || to.isEmpty() || amount == null) {
                    Notification.show("Preencha todos os campos!", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }

                double result = new com.example.Cambio.CurrencyService().convert(from, to, amount);
                resultLabel.setText(String.format("%.2f %s = %.2f %s",
                        amount, from.toUpperCase(), result, to.toUpperCase()));
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        VerticalLayout layout = new VerticalLayout();
        layout.add(fromField, toField, amountField, convertBtn, resultLabel);
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setAlignItems(FlexComponent.Alignment.STRETCH);

        dialog.add(layout);
        dialog.setWidth("400px");
        dialog.setHeight("auto");

        dialog.open();
    }
}
