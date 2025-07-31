package org.valle.process;

import lombok.extern.slf4j.Slf4j;
import org.valle.display.ShowPaths;
import org.valle.provide.GetAllPaths;

import java.util.List;

@Slf4j
public class GetAndShowPathsImpl implements GetAndShowPaths {

    private final GetAllPaths getAllPaths;
    private final ShowPaths showPaths;

    public GetAndShowPathsImpl(
            GetAllPaths getAllPaths,
            ShowPaths showPaths
    ) {
        this.getAllPaths = getAllPaths;
        this.showPaths = showPaths;
    }

    @Override
    public void execute() {
        List<String> paths = getAllPaths.provide();
        log.info("Found {} paths", paths.size());
        showPaths.display(paths);
    }
}
