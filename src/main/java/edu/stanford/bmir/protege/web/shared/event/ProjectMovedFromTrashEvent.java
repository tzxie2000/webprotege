package edu.stanford.bmir.protege.web.shared.event;

import com.google.web.bindery.event.shared.Event;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;

/**
 * Author: Matthew Horridge<br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 09/04/2013
 */
public class ProjectMovedFromTrashEvent extends WebProtegeEvent<ProjectMovedFromTrashHandler> {

    public static final transient Event.Type<ProjectMovedFromTrashHandler> TYPE = new Event.Type<ProjectMovedFromTrashHandler>();

    private ProjectId projectId;

    private ProjectMovedFromTrashEvent() {
    }

    public ProjectMovedFromTrashEvent(ProjectId projectId) {
        this.projectId = projectId;
    }

    public ProjectId getProjectId() {
        return projectId;
    }

    @Override
    public Event.Type<ProjectMovedFromTrashHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(ProjectMovedFromTrashHandler handler) {
        handler.handleProjectMovedFromTrash(this);
    }
}
