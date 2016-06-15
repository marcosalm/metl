/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.ui.views.design;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jumpmind.metl.core.model.Project;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.FieldFactory;
import org.jumpmind.metl.ui.common.Icons;
import org.jumpmind.metl.ui.views.DesignNavigator;
import org.jumpmind.metl.ui.views.UiConstants;
import org.jumpmind.vaadin.ui.common.ConfirmDialog;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.jumpmind.vaadin.ui.common.NotifyDialog;
import org.jumpmind.vaadin.ui.common.PromptDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Container.Indexed;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitEvent;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.data.sort.SortOrder;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.converter.StringToBooleanConverter;
import com.vaadin.data.util.filter.SimpleStringFilter;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.HeaderCell;
import com.vaadin.ui.Grid.HeaderRow;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.DateRenderer;
import com.vaadin.ui.renderers.HtmlRenderer;
import com.vaadin.ui.themes.ValoTheme;

public class ManageProjectsPanel extends VerticalLayout implements IUiPanel {

    private static final long serialVersionUID = 1L;

    final Logger log = LoggerFactory.getLogger(getClass());

    ApplicationContext context;

    DesignNavigator projectNavigator;

    Grid grid;

    BeanItemContainer<ProjectVersionItem> gridContainer;

    Button openProjectButton;

    Button newProjectButton;

    Button newVersionButton;

    Button editButton;

    Button removeButton;

    List<SortOrder> lastSortOrder;

    public ManageProjectsPanel(ApplicationContext context, DesignNavigator projectNavigator) {
        this.setSizeFull();
        this.context = context;
        this.projectNavigator = projectNavigator;

        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        openProjectButton = buttonBar.addButton("Open Project", Icons.PROJECT);
        openProjectButton.addClickListener(event -> open());

        newProjectButton = buttonBar.addButton("New Project", Icons.PROJECT);
        newProjectButton.addClickListener(event -> newProject());

        newVersionButton = buttonBar.addButton("New Version", Icons.VERSION);
        newVersionButton.addClickListener(event -> newVersion());
        newVersionButton.setEnabled(false);
        newVersionButton.setDescription("Not yet supported");

        editButton = buttonBar.addButton("Edit", FontAwesome.EDIT);
        editButton.addClickListener(event -> edit());

        removeButton = buttonBar.addButton("Remove", Icons.DELETE);
        removeButton.addClickListener(event -> removeProject());

        gridContainer = new BeanItemContainer<>(ProjectVersionItem.class);
        grid = new Grid();
        grid.setSizeFull();
        grid.setEditorEnabled(true);
        grid.setSelectionMode(SelectionMode.MULTI);

        grid.addColumn("name", String.class).setHeaderCaption("Name").setExpandRatio(2);
        grid.addColumn("description", String.class).setHeaderCaption("Description").setExpandRatio(1);
        grid.addColumn("version", String.class).setHeaderCaption("Version").setMaximumWidth(200);
        grid.addColumn("readOnly", Boolean.class).setHeaderCaption("Read Only").setMaximumWidth(100).setRenderer(new HtmlRenderer(), new StringToBooleanConverter() {
            private static final long serialVersionUID = 1L;

            protected String getTrueString() {
                return FontAwesome.CHECK.getHtml(); 
            };
            
            protected String getFalseString() {
                return "";
            };
        });
        grid.addColumn("createTime", Date.class).setHeaderCaption("Create Time").setWidth(185).setMaximumWidth(200)
                .setRenderer(new DateRenderer(UiConstants.DATETIME_FORMAT)).setEditable(false);

        grid.setContainerDataSource(gridContainer);
        grid.setEditorFieldFactory(new FieldFactory());
        grid.addSortListener(event -> {
            lastSortOrder = event.getSortOrder();
        });
        grid.addSelectionListener(event -> setButtonsEnabled());
        grid.addItemClickListener(event -> {
            if (!event.isDoubleClick()) {
                if (!event.isShiftKey()) {
                    Collection<Object> all = grid.getSelectedRows();
                    for (Object selected : all) {
                        if (!selected.equals(event.getItemId())) {
                            grid.deselect(selected);
                        }
                    }
                }
                if (grid.getSelectedRows().contains(event.getItemId())) {
                    grid.deselect(event.getItemId());
                } else {
                    grid.select(event.getItemId());
                }
            }
        });

        grid.getEditorFieldGroup().addCommitHandler(new FieldGroup.CommitHandler() {

            private static final long serialVersionUID = 1L;

            @Override
            public void preCommit(CommitEvent commitEvent) throws CommitException {
            }

            @Override
            public void postCommit(CommitEvent commitEvent) throws CommitException {
                ProjectVersionItem item = (ProjectVersionItem) grid.getEditedItemId();
                IConfigurationService configurationService = context.getConfigurationService();
                configurationService.save(item.version);
                configurationService.save(item.project);
                @SuppressWarnings("unchecked")
                Collection<ProjectVersionItem> items = (Collection<ProjectVersionItem>)grid.getContainerDataSource().getItemIds();
                for (ProjectVersionItem projectVersion : items) {
                    if (item.project.equals(projectVersion.project)) {
                        projectVersion.project = item.project;
                    }
                }
                grid.markAsDirty();
            }
        });

        HeaderRow filteringHeader = grid.appendHeaderRow();
        HeaderCell logTextFilterCell = filteringHeader.getCell("name");
        TextField filterField = new TextField();
        filterField.setInputPrompt("Filter");
        filterField.addStyleName(ValoTheme.TEXTFIELD_TINY);
        filterField.setWidth("100%");

        filterField.addTextChangeListener(change -> {
            gridContainer.removeContainerFilters("name");
            if (!change.getText().isEmpty()) {
                gridContainer.addContainerFilter(new SimpleStringFilter("name", change.getText(), true, false));
            }
        });
        logTextFilterCell.setComponent(filterField);

        addComponent(grid);
        setExpandRatio(grid, 1);

        setButtonsEnabled();

    }

    protected void sort() {
        if (lastSortOrder == null) {
            lastSortOrder = new ArrayList<>();
            lastSortOrder.add(new SortOrder("version", SortDirection.DESCENDING));
            lastSortOrder.add(new SortOrder("name", SortDirection.ASCENDING));
        }
        grid.setSortOrder(lastSortOrder);
    }

    protected void deselectAll() {
        Collection<Object> all = grid.getSelectedRows();
        for (Object selected : all) {
            grid.deselect(selected);
        }
    }

    protected void setButtonsEnabled() {
        int numberSelected = grid.getSelectionModel().getSelectedRows().size();
        boolean selected = numberSelected > 0;
        removeButton.setEnabled(selected);
        openProjectButton.setEnabled(selected);
        editButton.setEnabled(selected);
        newVersionButton.setEnabled(numberSelected == 1);

        boolean currentlyEditing = grid.getEditedItemId() != null;
        if (currentlyEditing) {
            removeButton.setEnabled(false);
            openProjectButton.setEnabled(false);
            editButton.setEnabled(false);
            newProjectButton.setEnabled(false);
            newVersionButton.setEnabled(false);
        }
    }

    @Override
    public boolean closing() {
        return true;
    }

    @Override
    public void selected() {
        refresh();
    }

    @Override
    public void deselected() {
    }

    protected void open() {
        Collection<Object> selected = grid.getSelectedRows();
        for (Object object : selected) {
            projectNavigator.addProjectVersion(((ProjectVersionItem) object).version);
        }
    }

    protected void refresh() {
        Collection<Object> selected = grid.getSelectedRows();
        gridContainer.removeAllItems();
        IConfigurationService configurationService = context.getConfigurationService();

        List<Project> projects = configurationService.findProjects();
        for (Project project : projects) {
            List<ProjectVersion> versions = project.getProjectVersions();
            for (ProjectVersion version : versions) {
                grid.getContainerDataSource().addItem(new ProjectVersionItem(project, version));
            }
        }

        for (Object s : selected) {
            if (grid.getContainerDataSource().containsId(s)) {
                grid.select(s);
            }
        }

        setButtonsEnabled();

        sort();

    }

    protected void edit() {
        Collection<Object> selected = grid.getSelectedRows();
        if (selected.size() > 0) {
            grid.editItem(selected.iterator().next());
        }
    }

    protected void newVersion() {
        Collection<Object> selected = grid.getSelectedRows();
        if (selected.size() == 1) {
            IConfigurationService configurationService = context.getConfigurationService();
            ProjectVersionItem item = (ProjectVersionItem) selected.iterator().next();
            PromptDialog.prompt("New Version", String.format("Copying version '%s' of '%s'.  What do you want to name new version?",
                    item.version.getVersionLabel(), item.project.getName()), (newVersionLabel) -> {
                        if (StringUtils.isNotBlank(newVersionLabel)) {
                            ProjectVersion newVersion = configurationService.saveNewVersion(newVersionLabel, item.version);
                            Indexed indexed = grid.getContainerDataSource();
                            indexed.addItemAfter(item, new ProjectVersionItem(item.project, newVersion));
                            return true;
                        } else {
                            NotifyDialog.show("Please specify a version number", "Please specify a version number", null,
                                    Type.WARNING_MESSAGE);
                            return false;
                        }
                    });
        }
    }

    protected void newProject() {
        deselectAll();
        Project project = new Project();
        project.setName("New Project");
        ProjectVersion version = new ProjectVersion();
        version.setVersionLabel("1.0.0");
        version.setProject(project);
        project.getProjectVersions().add(version);
        IConfigurationService configurationService = context.getConfigurationService();
        configurationService.save(project);
        configurationService.save(version);
        ProjectVersionItem item = new ProjectVersionItem(project, version);
        grid.getContainerDataSource().addItem(item);
        grid.select(item);
    }

    protected void removeProject() {
        ConfirmDialog.show("Delete Project(s)?", "Are you sure you want to delete the selected project(s)?", () -> {
            Collection<Object> selected = grid.getSelectedRows();
            for (Object object : selected) {
                ProjectVersionItem item = (ProjectVersionItem) object;
                grid.getContainerDataSource().removeItem(item);
                item.version.setDeleted(true);
                context.getConfigurationService().save(item.version);
            }
            sort();
            setButtonsEnabled();
            return true;
        });
    }

    public static class ProjectVersionItem implements Serializable {

        private static final long serialVersionUID = 1L;

        Project project;

        ProjectVersion version;

        public ProjectVersionItem() {
        }

        public ProjectVersionItem(Project project, ProjectVersion version) {
            this.project = project;
            this.version = version;
        }

        public String getName() {
            return project.getName();
        }

        public void setName(String projectName) {
            this.project.setName(projectName);
        }

        public String getVersion() {
            return version.getVersionLabel();
        }

        public void setVersion(String version) {
            this.version.setVersionLabel(version);
        }

        public String getDescription() {
            return this.version.getDescription();
        }

        public void setDescription(String description) {
            this.version.setDescription(description);
        }

        public Date getCreateTime() {
            return this.version.getCreateTime();
        }
        
        public boolean isReadOnly() {
            return this.version.isReadOnly();
        }
        
        public void setReadOnly(boolean readOnly) {
            this.version.setReadOnly(readOnly);
        }

        @Override
        public int hashCode() {
            return version.getId().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ProjectVersionItem) {
                return ((ProjectVersionItem) obj).version.getId().equals(version.getId());
            } else {
                return false;
            }
        }

    }

}
