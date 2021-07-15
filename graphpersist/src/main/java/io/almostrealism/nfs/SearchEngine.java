package io.almostrealism.nfs;

import io.almostrealism.code.Resource;

public interface SearchEngine {
	Iterable<Resource> search(String path);
}
