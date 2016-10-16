/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal.artifacts.repositories;

import org.gradle.api.Action;
import org.gradle.api.artifacts.repositories.PasswordCredentials;
import org.gradle.api.credentials.AwsCredentials;
import org.gradle.api.credentials.Credentials;
import org.gradle.internal.Cast;
import org.gradle.internal.artifacts.repositories.AuthenticationSupportedInternal;
import org.gradle.internal.credentials.DefaultAwsCredentials;
import org.gradle.internal.reflect.Instantiator;

public abstract class AbstractAuthenticationSupportedRepository extends AbstractArtifactRepository implements AuthenticationSupportedInternal {

    private Credentials credentials;
    private final Instantiator instantiator;

    AbstractAuthenticationSupportedRepository(Instantiator instantiator) {
        this.instantiator = instantiator;
    }

    @Override
    public PasswordCredentials getCredentials() {
        if (credentials == null) {
            return setCredentials(PasswordCredentials.class);
        } else if (credentials instanceof PasswordCredentials) {
            return Cast.uncheckedCast(credentials);
        } else {
            throw new IllegalStateException("Can not use getCredentials() method when not using PasswordCredentals; please use getCredentials(Class)");
        }
    }

    @Override
    public <T extends Credentials> T getCredentials(Class<T> credentialsType) {
        if (credentials == null) {
            return setCredentials(credentialsType);
        } else if (credentialsType.isInstance(credentials)) {
            return Cast.uncheckedCast(credentials);
        } else {
            throw new IllegalArgumentException(String.format("Given credentials type '%s' does not match actual type '%s'", credentialsType.getName(), getPublicType(credentials.getClass()).getName()));
        }
    }

    public void credentials(Action<? super PasswordCredentials> action) {
        if (credentials != null && !(credentials instanceof PasswordCredentials)) {
            throw new IllegalStateException("Can not use credentials(Action) method when not using PasswordCredentals; please use credentials(Class, Action)");
        }
        credentials(PasswordCredentials.class, action);
    }

    public <T extends Credentials> void credentials(Class<T> credentialsType, Action<? super T> action) throws IllegalStateException {
        action.execute(getCredentials(credentialsType));
    }

    private <T extends Credentials> T setCredentials(Class<T> clazz) {
        T t = newCredentials(clazz);
        credentials = t;
        return t;
    }

    private <T extends Credentials> T newCredentials(Class<T> clazz) {
        return instantiator.newInstance(getImplType(clazz));
    }

    public Credentials getConfiguredCredentials() {
        return credentials;
    }

    // Mappings between public and impl types
    // If the list of mappings grows we should move it to a data structure

    private static <T extends Credentials> Class<? extends T> getImplType(Class<T> publicType) {
        if (publicType == PasswordCredentials.class) {
            return Cast.uncheckedCast(DefaultPasswordCredentials.class);
        } else if (publicType == AwsCredentials.class) {
            return Cast.uncheckedCast(DefaultAwsCredentials.class);
        } else {
            throw new IllegalArgumentException(String.format("Unknown credentials type: '%s' (supported types: %s and %s).", publicType.getName(), PasswordCredentials.class.getName(), AwsCredentials.class.getName()));
        }
    }

    private static <T extends Credentials> Class<? super T> getPublicType(Class<T> implType) {
        if (PasswordCredentials.class.isAssignableFrom(implType)) {
            return Cast.uncheckedCast(PasswordCredentials.class);
        } else if (AwsCredentials.class.isAssignableFrom(implType)) {
            return Cast.uncheckedCast(AwsCredentials.class);
        } else {
            throw new IllegalArgumentException(String.format("Unknown credentials implementation type: '%s' (supported types: %s and %s).", implType.getName(), DefaultPasswordCredentials.class.getName(), DefaultAwsCredentials.class.getName()));
        }
    }
}