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
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;


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

        add(new ViewToolbar("Task List", ViewToolbar.group(description, dueDate, createBtn, exportPdfBtn)));
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
            List<String> tasks = Optional.ofNullable(taskService.findAllTasks())
                    .orElse(List.of())
                    .stream()
                    .map(Task::getDescription)
                    .filter(description -> !description.isBlank())
                    .toList();

            byte[] pdfBytes = com.example.pdf.PdfGenerator
                    .generateTasksPdfBytes(tasks, "Lista de Tarefas");

            com.vaadin.flow.server.StreamResource resource =
                    new com.vaadin.flow.server.StreamResource("tarefas.pdf",
                            () -> new ByteArrayInputStream(pdfBytes));

            resource.setContentType("application/pdf");
            resource.getHeaders().put("Content-Disposition", "attachment; filename=tarefas.pdf");

            com.vaadin.flow.component.html.Anchor downloadLink =
                    new com.vaadin.flow.component.html.Anchor(resource, "");
            downloadLink.getElement().setAttribute("download", true);
            downloadLink.getElement().getStyle().set("display", "none");

            getElement().appendChild(downloadLink.getElement());
            downloadLink.getElement().executeJs("this.click();");

            Notification.show("PDF gerado com sucesso!", 3000, Notification.Position.MIDDLE);
        } catch (Exception e) {
            Notification.show("Erro ao gerar PDF: " + e.getMessage(),
                    5000, Notification.Position.MIDDLE);
        }
    }
}
